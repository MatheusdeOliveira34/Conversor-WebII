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

export default { getFormats, convert }
