/**
 * Download do arquivo convertido, que chega em base64 no ConversionResponse.
 * Nada é gravado no servidor: o arquivo só existe na memória do navegador.
 */

/** Decodifica base64 em um Blob com o MIME informado pelo back-end. */
export function base64ToBlob (base64, contentType) {
  const binary = atob(base64)
  const bytes = new Uint8Array(binary.length)
  for (let i = 0; i < binary.length; i++) {
    bytes[i] = binary.charCodeAt(i)
  }
  return new Blob([bytes], { type: contentType })
}

/** Cria uma URL local (blob:) para o conteúdo; guarde-a para "Baixar novamente". */
export function createDownloadUrl (base64, contentType) {
  return URL.createObjectURL(base64ToBlob(base64, contentType))
}

/** Dispara o download da URL com o nome de arquivo sugerido. */
export function triggerDownload (url, fileName) {
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  link.style.display = 'none'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}
