import { base64ToBlob, createDownloadUrl, triggerDownload } from '@/utils/download'

describe('download', () => {
  afterEach(() => {
    jest.restoreAllMocks()
    delete URL.createObjectURL
  })

  it('base64ToBlob decodifica para os mesmos bytes com o MIME informado', async () => {
    const blob = base64ToBlob(btoa('%PDF-1.7'), 'application/pdf')

    expect(blob.type).toBe('application/pdf')
    expect(blob.size).toBe(8)
    const text = await new Promise(resolve => {
      const reader = new FileReader()
      reader.onload = () => resolve(reader.result)
      reader.readAsText(blob)
    })
    expect(text).toBe('%PDF-1.7')
  })

  it('createDownloadUrl gera uma URL blob: local', () => {
    URL.createObjectURL = jest.fn(() => 'blob:http://localhost/123')

    expect(createDownloadUrl(btoa('abc'), 'text/plain')).toBe('blob:http://localhost/123')
    expect(URL.createObjectURL.mock.calls[0][0]).toBeInstanceOf(Blob)
  })

  it('triggerDownload clica em um link com o nome do arquivo e o remove', () => {
    const click = jest.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(function () {
      expect(this.getAttribute('href')).toBe('blob:http://localhost/123')
      expect(this.download).toBe('relatorio.pdf')
      expect(document.body.contains(this)).toBe(true)
    })

    triggerDownload('blob:http://localhost/123', 'relatorio.pdf')

    expect(click).toHaveBeenCalledTimes(1)
    expect(document.querySelector('a')).toBeNull()
  })
})
