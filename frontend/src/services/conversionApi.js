import axios from 'axios'

/**
 * Cliente HTTP da API de conversões (contrato em documentacao/arquitetura.md, seção 5).
 * A URL base vem de VUE_APP_API_URL (frontend/.env.development).
 */
export const BASE_PATH = '/api/v1/conversions'

export const http = axios.create({
  baseURL: process.env.VUE_APP_API_URL
})

/**
 * GET /api/v1/conversions/formats
 * @returns {Promise<Array<{sourceFormat: string, targetFormats: string[]}>>}
 */
export function getFormats () {
  return http.get(`${BASE_PATH}/formats`).then(response => response.data)
}

/**
 * POST /api/v1/conversions (multipart/form-data com `file` e `targetFormat`)
 * @param {File} file arquivo escolhido pelo usuário
 * @param {string} targetFormat PDF, DOCX ou XLSX
 * @returns {Promise<{fileName: string, contentType: string, sourceFormat: string,
 *   targetFormat: string, sizeBytes: number, durationMs: number, contentBase64: string}>}
 */
export function convert (file, targetFormat) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('targetFormat', targetFormat)
  return http.post(BASE_PATH, formData).then(response => response.data)
}

/** Mensagem quando não há resposta do servidor (UC01 E5). */
export const SERVER_UNAVAILABLE = 'Servidor indisponível. Tente novamente.'

/** Mensagem para qualquer erro sem `message` no corpo (UC01 E4). */
export const GENERIC_ERROR = 'Erro inesperado, tente novamente.'

/** Usadas só se o corpo não for um ErrorResponse (ex.: limite do servidor antes do handler). */
const MESSAGES_BY_STATUS = {
  413: 'Arquivo maior que 10 MB.',
  415: 'Formato não suportado. Use PDF, DOCX ou XLSX.'
}

/**
 * Traduz um erro do Axios na mensagem mostrada ao usuário (RF09):
 * usa o `message` do ErrorResponse quando existe; senão, uma mensagem pelo status.
 * @param {Error} error
 * @returns {string}
 */
export function errorMessage (error) {
  const response = error && error.response
  if (!response) {
    return SERVER_UNAVAILABLE
  }
  const data = response.data
  if (data && typeof data.message === 'string' && data.message.trim()) {
    return data.message
  }
  return MESSAGES_BY_STATUS[response.status] || GENERIC_ERROR
}

export default { getFormats, convert, errorMessage }
