import { shallowMount } from '@vue/test-utils'
import App from '@/App.vue'

describe('App.vue', () => {
  it('mostra o cabeçalho com título e subtítulo', () => {
    const wrapper = shallowMount(App)

    expect(wrapper.find('h1').text()).toBe('FileConverter')
    expect(wrapper.find('.app-subtitle').text()).toBe('PDF • Word • Excel')
  })

  it('começa no estado inicial da tela', () => {
    const wrapper = shallowMount(App)

    expect(wrapper.vm.$data).toEqual({
      file: null,
      targetFormat: null,
      conversions: [],
      loading: false,
      result: null,
      error: null
    })
  })
})

describe('App.vue — seleção de arquivo', () => {
  it('mostra nome, tamanho e tipo do arquivo escolhido (RF07)', async () => {
    const wrapper = shallowMount(App)
    const file = new File(['x'.repeat(2048)], 'relatorio.docx')

    wrapper.findComponent({ name: 'FileDropzone' }).vm.$emit('file-selected', file, 'DOCX')
    await wrapper.vm.$nextTick()

    expect(wrapper.find('.file-card__name').text()).toBe('relatorio.docx')
    expect(wrapper.find('.file-card__size').text()).toBe('2 KB')
    expect(wrapper.find('.format-tag').text()).toBe('DOCX')
    expect(wrapper.findComponent({ name: 'FileDropzone' }).exists()).toBe(false)
  })
})
