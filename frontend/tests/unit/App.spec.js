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
