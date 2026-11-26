import {
  http,
  getFormats,
  convert,
  errorMessage,
  BASE_PATH,
  GENERIC_ERROR,
  SERVER_UNAVAILABLE
} from '@/services/conversionApi'

describe('conversionApi', () => {
  afterEach(() => {
    jest.restoreAllMocks()
  })

  it('getFormats chama GET /api/v1/conversions/formats e devolve o corpo', async () => {
    const formats = [{ sourceFormat: 'DOCX', targetFormats: ['PDF', 'XLSX'] }]
    const get = jest.spyOn(http, 'get').mockResolvedValue({ data: formats })

    await expect(getFormats()).resolves.toEqual(formats)
    expect(get).toHaveBeenCalledWith('/api/v1/conversions/formats')
  })

  it('convert envia multipart com file e targetFormat', async () => {
    const response = { fileName: 'relatorio.pdf', contentType: 'application/pdf' }
    const post = jest.spyOn(http, 'post').mockResolvedValue({ data: response })
    const file = new File(['conteudo'], 'relatorio.docx')

    await expect(convert(file, 'PDF')).resolves.toEqual(response)

    const [url, body] = post.mock.calls[0]
    expect(url).toBe(BASE_PATH)
    expect(body).toBeInstanceOf(FormData)
    expect(body.get('file').name).toBe('relatorio.docx')
    expect(body.get('targetFormat')).toBe('PDF')
  })

  describe('errorMessage', () => {
    it('usa o message do ErrorResponse', () => {
      const error = {
        response: {
          status: 422,
          data: { code: 'CORRUPTED_FILE', message: 'O arquivo está corrompido ou não pôde ser lido.' }
        }
      }

      expect(errorMessage(error)).toBe('O arquivo está corrompido ou não pôde ser lido.')
    })

    it('sem resposta do servidor diz que está indisponível', () => {
      expect(errorMessage(new Error('Network Error'))).toBe(SERVER_UNAVAILABLE)
    })

    it('sem message no corpo usa a mensagem do status ou a genérica', () => {
      expect(errorMessage({ response: { status: 413, data: '' } })).toBe('Arquivo maior que 10 MB.')
      expect(errorMessage({ response: { status: 500, data: { message: '' } } })).toBe(GENERIC_ERROR)
    })
  })
})
