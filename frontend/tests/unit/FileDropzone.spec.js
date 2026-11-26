import { shallowMount } from '@vue/test-utils'
import FileDropzone from '@/components/FileDropzone.vue'
import { MAX_FILE_SIZE, MESSAGES } from '@/utils/fileFormat'

function makeFile (name, size = 1024) {
  const file = new File(['conteudo'], name)
  Object.defineProperty(file, 'size', { value: size })
  return file
}

function selectFile (wrapper, file) {
  const input = wrapper.find('input[type="file"]')
  Object.defineProperty(input.element, 'files', { value: [file], configurable: true })
  return input.trigger('change')
}

describe('FileDropzone.vue', () => {
  it('CT12: com .png emite erro e não emite file-selected', async () => {
    const wrapper = shallowMount(FileDropzone)

    await selectFile(wrapper, makeFile('foto.png'))

    expect(wrapper.emitted('file-selected')).toBeUndefined()
    expect(wrapper.emitted('error')[0]).toEqual([MESSAGES.UNSUPPORTED_TYPE])
    expect(wrapper.find('.file-dropzone__error').text()).toContain(MESSAGES.UNSUPPORTED_TYPE)
    expect(wrapper.find('.file-dropzone__status').attributes('aria-live')).toBe('polite')
  })

  it('rejeita arquivo acima de 10 MB', async () => {
    const wrapper = shallowMount(FileDropzone)

    await selectFile(wrapper, makeFile('grande.pdf', MAX_FILE_SIZE + 1))

    expect(wrapper.emitted('file-selected')).toBeUndefined()
    expect(wrapper.emitted('error')[0]).toEqual([MESSAGES.TOO_LARGE])
  })

  it('aceita .docx pelo clique e emite o arquivo com o formato', async () => {
    const wrapper = shallowMount(FileDropzone)
    const file = makeFile('relatorio.docx')

    await selectFile(wrapper, file)

    expect(wrapper.emitted('file-selected')[0]).toEqual([file, 'DOCX'])
    expect(wrapper.emitted('error')).toBeUndefined()
  })

  it('aceita arquivo solto na área (arrastar e soltar)', async () => {
    const wrapper = shallowMount(FileDropzone)
    const file = makeFile('PLANILHA.XLSX')

    await wrapper.find('label').trigger('drop', { dataTransfer: { files: [file] } })

    expect(wrapper.emitted('file-selected')[0]).toEqual([file, 'XLSX'])
  })

  it('limpa a mensagem de erro quando um arquivo válido é escolhido', async () => {
    const wrapper = shallowMount(FileDropzone)

    await selectFile(wrapper, makeFile('foto.png'))
    await selectFile(wrapper, makeFile('relatorio.pdf'))

    expect(wrapper.find('.file-dropzone__error').exists()).toBe(false)
  })

  it('a área de upload é um label ligado ao input de arquivo', () => {
    const wrapper = shallowMount(FileDropzone)
    const input = wrapper.find('input[type="file"]')

    expect(wrapper.find('label').attributes('for')).toBe(input.attributes('id'))
    expect(input.attributes('accept')).toBe('.pdf,.docx,.xlsx')
  })
})
