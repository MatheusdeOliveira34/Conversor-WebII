import {
  formatFileSize,
  formatFromFileName,
  getExtension,
  MAX_FILE_SIZE,
  MESSAGES,
  validateFile
} from '@/utils/fileFormat'

describe('fileFormat', () => {
  it('lê a extensão sem diferenciar maiúsculas', () => {
    expect(getExtension('Relatorio.Final.DOCX')).toBe('docx')
    expect(getExtension('sem-extensao')).toBe('')
    expect(getExtension('.pdf')).toBe('')
  })

  it('mapeia extensão para o formato do back-end', () => {
    expect(formatFromFileName('a.pdf')).toBe('PDF')
    expect(formatFromFileName('a.docx')).toBe('DOCX')
    expect(formatFromFileName('a.xlsx')).toBe('XLSX')
    expect(formatFromFileName('a.doc')).toBeNull()
  })

  it('valida tipo, tamanho máximo e arquivo vazio', () => {
    expect(validateFile({ name: 'a.txt', size: 10 })).toEqual({ format: null, error: MESSAGES.UNSUPPORTED_TYPE })
    expect(validateFile({ name: 'a.pdf', size: MAX_FILE_SIZE + 1 })).toEqual({ format: null, error: MESSAGES.TOO_LARGE })
    expect(validateFile({ name: 'a.pdf', size: 0 })).toEqual({ format: null, error: MESSAGES.EMPTY })
    expect(validateFile({ name: 'a.pdf', size: MAX_FILE_SIZE })).toEqual({ format: 'PDF', error: null })
  })

  it('formata o tamanho em B, KB e MB', () => {
    expect(formatFileSize(512)).toBe('512 B')
    expect(formatFileSize(48 * 1024)).toBe('48 KB')
    expect(formatFileSize(2.4 * 1024 * 1024)).toBe('2,4 MB')
  })
})
