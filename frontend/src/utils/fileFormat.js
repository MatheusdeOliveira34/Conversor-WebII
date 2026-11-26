/**
 * Regras de arquivo do front-end, espelhando o back-end (FileFormat e application.yml).
 * A validação aqui é conveniência para o usuário; a que vale é a do servidor.
 */

/** Limite de upload (RNF06): 10 MB, igual a spring.servlet.multipart.max-file-size. */
export const MAX_FILE_SIZE = 10 * 1024 * 1024

/** Extensão (minúscula, sem ponto) → formato do enum FileFormat do back-end. */
export const EXTENSION_TO_FORMAT = Object.freeze({
  pdf: 'PDF',
  docx: 'DOCX',
  xlsx: 'XLSX'
})

/** Valor do atributo `accept` do <input type="file">. */
export const ACCEPTED_EXTENSIONS = Object.keys(EXTENSION_TO_FORMAT)
  .map(extension => `.${extension}`)
  .join(',')

export const MESSAGES = Object.freeze({
  UNSUPPORTED_TYPE: 'Formato não suportado. Use PDF, DOCX ou XLSX.',
  TOO_LARGE: 'Arquivo maior que 10 MB.',
  EMPTY: 'O arquivo está vazio.'
})

/** Extensão do nome do arquivo, em minúsculas e sem o ponto ('' se não houver). */
export function getExtension (fileName) {
  const name = String(fileName || '')
  const dot = name.lastIndexOf('.')
  return dot > 0 ? name.slice(dot + 1).toLowerCase() : ''
}

/** Formato (PDF, DOCX, XLSX) a partir do nome do arquivo, ou null se não suportado. */
export function formatFromFileName (fileName) {
  return EXTENSION_TO_FORMAT[getExtension(fileName)] || null
}

/**
 * Valida extensão e tamanho no navegador (UC01, passo 2).
 * @param {File} file
 * @returns {{ format: string|null, error: string|null }}
 */
export function validateFile (file) {
  const format = formatFromFileName(file && file.name)
  if (!format) {
    return { format: null, error: MESSAGES.UNSUPPORTED_TYPE }
  }
  if (file.size > MAX_FILE_SIZE) {
    return { format: null, error: MESSAGES.TOO_LARGE }
  }
  if (file.size === 0) {
    return { format: null, error: MESSAGES.EMPTY }
  }
  return { format, error: null }
}

/** Tamanho legível em português: 512 B, 48 KB, 2,4 MB. */
export function formatFileSize (bytes) {
  if (bytes < 1024) {
    return `${bytes} B`
  }
  if (bytes < 1024 * 1024) {
    return `${Math.round(bytes / 1024)} KB`
  }
  return `${(bytes / (1024 * 1024)).toFixed(1).replace('.', ',')} MB`
}

/** Rótulo mostrado ao usuário para cada formato (interface.md: "PDF • Word • Excel"). */
export const FORMAT_LABELS = Object.freeze({
  PDF: 'PDF',
  DOCX: 'Word',
  XLSX: 'Excel'
})

/**
 * Destinos válidos para a origem, a partir da resposta de GET /formats (RF04).
 * @param {Array<{sourceFormat: string, targetFormats: string[]}>} conversions
 * @param {string|null} sourceFormat
 * @returns {string[]}
 */
export function targetFormatsFor (conversions, sourceFormat) {
  const entry = (conversions || []).find(item => item.sourceFormat === sourceFormat)
  return entry ? entry.targetFormats.filter(target => target !== sourceFormat) : []
}
