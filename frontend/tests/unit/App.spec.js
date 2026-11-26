import { shallowMount } from '@vue/test-utils'
import App, { FORMATS_ERROR } from '@/App.vue'
import { getFormats } from '@/services/conversionApi'

jest.mock('@/services/conversionApi', () => ({
  getFormats: jest.fn(),
  convert: jest.fn()
}))

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
    getFormats.mockReset()
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
      error: null
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
})
