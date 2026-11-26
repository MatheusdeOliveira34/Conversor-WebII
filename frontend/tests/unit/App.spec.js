import { shallowMount } from '@vue/test-utils'
import App, { FORMATS_ERROR } from '@/App.vue'
import { convert, getFormats } from '@/services/conversionApi'
import { createDownloadUrl, triggerDownload } from '@/utils/download'

jest.mock('@/services/conversionApi', () => ({
  ...jest.requireActual('@/services/conversionApi'),
  getFormats: jest.fn(),
  convert: jest.fn()
}))

jest.mock('@/utils/download', () => ({
  createDownloadUrl: jest.fn(() => 'blob:http://localhost/1'),
  triggerDownload: jest.fn()
}))

const RESPONSE = {
  fileName: 'relatorio.pdf',
  contentType: 'application/pdf',
  sourceFormat: 'DOCX',
  targetFormat: 'PDF',
  sizeBytes: 8,
  durationMs: 312,
  contentBase64: 'JVBERi0xLjc='
}

const CONVERSIONS = [
  { sourceFormat: 'PDF', targetFormats: ['DOCX', 'XLSX'] },
  { sourceFormat: 'DOCX', targetFormats: ['PDF', 'XLSX'] },
  { sourceFormat: 'XLSX', targetFormats: ['PDF', 'DOCX'] }
]

const flushPromises = () => new Promise(resolve => setTimeout(resolve))

async function mountLoaded () {
  const wrapper = shallowMount(App)
  await flushPromises()
  return wrapper
}

async function selectFile (wrapper, name = 'relatorio.docx', format = 'DOCX') {
  const file = new File(['x'.repeat(2048)], name)
  wrapper.findComponent({ name: 'FileDropzone' }).vm.$emit('file-selected', file, format)
  await wrapper.vm.$nextTick()
  return file
}

describe('App.vue', () => {
  beforeEach(() => {
    jest.clearAllMocks()
    getFormats.mockResolvedValue(CONVERSIONS)
  })

  it('mostra o cabeçalho com título e subtítulo', () => {
    const wrapper = shallowMount(App)

    expect(wrapper.find('h1').text()).toBe('FileConverter')
    expect(wrapper.find('.app-subtitle').text()).toBe('PDF • Word • Excel')
  })

  it('começa no estado inicial da tela, carregando os formatos', () => {
    const wrapper = shallowMount(App)

    expect(wrapper.vm.$data).toEqual({
      file: null,
      targetFormat: null,
      conversions: [],
      loadingFormats: true,
      formatsError: null,
      loading: false,
      result: null,
      error: null,
      downloadUrl: null
    })
    expect(wrapper.findComponent({ name: 'FileDropzone' }).props('loading')).toBe(true)
  })

  describe('UC02 — formatos suportados', () => {
    it('carrega as conversões ao abrir e libera o upload', async () => {
      const wrapper = await mountLoaded()

      expect(getFormats).toHaveBeenCalledTimes(1)
      expect(wrapper.vm.conversions).toEqual(CONVERSIONS)
      const dropzone = wrapper.findComponent({ name: 'FileDropzone' })
      expect(dropzone.props('loading')).toBe(false)
      expect(dropzone.props('disabled')).toBe(false)
    })

    it('E1: back-end indisponível mostra erro e mantém o upload desabilitado', async () => {
      getFormats.mockRejectedValue(new Error('Network Error'))
      const wrapper = await mountLoaded()

      expect(wrapper.find('[role="alert"]').text()).toBe(FORMATS_ERROR)
      expect(wrapper.findComponent({ name: 'FileDropzone' }).props('disabled')).toBe(true)
    })
  })

  describe('seleção de arquivo', () => {
    it('mostra nome, tamanho e tipo do arquivo escolhido (RF07)', async () => {
      const wrapper = await mountLoaded()

      await selectFile(wrapper)

      expect(wrapper.find('.file-card__name').text()).toBe('relatorio.docx')
      expect(wrapper.find('.file-card__size').text()).toBe('2 KB')
      expect(wrapper.find('.format-tag').text()).toBe('DOCX')
      expect(wrapper.findComponent({ name: 'FileDropzone' }).exists()).toBe(false)
    })

    it('passa ao seletor só os destinos válidos para a origem (RF04)', async () => {
      const wrapper = await mountLoaded()

      await selectFile(wrapper, 'planilha.xlsx', 'XLSX')

      const selector = wrapper.findComponent({ name: 'FormatSelector' })
      expect(selector.props('options')).toEqual(['PDF', 'DOCX'])
    })

    it('guarda o destino escolhido pelo v-model', async () => {
      const wrapper = await mountLoaded()
      await selectFile(wrapper)

      wrapper.findComponent({ name: 'FormatSelector' }).vm.$emit('input', 'PDF')

      expect(wrapper.vm.targetFormat).toBe('PDF')
    })
  })

  describe('UC01 — converter e baixar', () => {
    async function readyToConvert () {
      const wrapper = await mountLoaded()
      await selectFile(wrapper)
      wrapper.findComponent({ name: 'FormatSelector' }).vm.$emit('input', 'PDF')
      await wrapper.vm.$nextTick()
      return wrapper
    }

    it('botão Converter fica desabilitado até escolher o destino', async () => {
      const wrapper = await mountLoaded()
      await selectFile(wrapper)

      expect(wrapper.find('.btn--primary').attributes('disabled')).toBeDefined()

      wrapper.findComponent({ name: 'FormatSelector' }).vm.$emit('input', 'PDF')
      await wrapper.vm.$nextTick()
      expect(wrapper.find('.btn--primary').attributes('disabled')).toBeUndefined()
    })

    it('mostra o spinner e "Convertendo…" enquanto envia (RF08)', async () => {
      convert.mockReturnValue(new Promise(() => {}))
      const wrapper = await readyToConvert()

      await wrapper.find('.btn--primary').trigger('click')

      const button = wrapper.find('.btn--primary')
      expect(button.text()).toBe('Convertendo…')
      expect(button.find('.spinner').exists()).toBe(true)
      expect(button.attributes('disabled')).toBeDefined()
      expect(wrapper.findComponent({ name: 'FormatSelector' }).props('disabled')).toBe(true)
    })

    it('envia arquivo e destino, baixa automaticamente e mostra o resultado', async () => {
      convert.mockResolvedValue(RESPONSE)
      const wrapper = await readyToConvert()
      const file = wrapper.vm.file

      await wrapper.find('.btn--primary').trigger('click')
      await flushPromises()

      expect(convert).toHaveBeenCalledWith(file, 'PDF')
      expect(createDownloadUrl).toHaveBeenCalledWith('JVBERi0xLjc=', 'application/pdf')
      expect(triggerDownload).toHaveBeenCalledWith('blob:http://localhost/1', 'relatorio.pdf')

      const panel = wrapper.findComponent({ name: 'ResultPanel' })
      expect(panel.props('result')).toEqual({ ...RESPONSE, contentBase64: undefined })
      expect(panel.props('result')).not.toHaveProperty('contentBase64')
      expect(wrapper.find('.btn--primary').exists()).toBe(false)
    })

    it('"Baixar novamente" reutiliza a mesma URL', async () => {
      convert.mockResolvedValue(RESPONSE)
      const wrapper = await readyToConvert()
      await wrapper.find('.btn--primary').trigger('click')
      await flushPromises()

      wrapper.findComponent({ name: 'ResultPanel' }).vm.$emit('download')

      expect(triggerDownload).toHaveBeenCalledTimes(2)
      expect(createDownloadUrl).toHaveBeenCalledTimes(1)
    })

    it('mostra o message do ErrorResponse e permite tentar novamente', async () => {
      convert.mockRejectedValueOnce({
        response: { status: 422, data: { code: 'CORRUPTED_FILE', message: 'Arquivo corrompido.' } }
      })
      const wrapper = await readyToConvert()

      await wrapper.find('.btn--primary').trigger('click')
      await flushPromises()

      const panel = wrapper.findComponent({ name: 'ResultPanel' })
      expect(panel.props('error')).toBe('Arquivo corrompido.')
      expect(triggerDownload).not.toHaveBeenCalled()

      convert.mockResolvedValue(RESPONSE)
      panel.vm.$emit('retry')
      await flushPromises()

      expect(convert).toHaveBeenCalledTimes(2)
      expect(wrapper.vm.error).toBeNull()
      expect(wrapper.vm.result.fileName).toBe('relatorio.pdf')
    })
  })
})
