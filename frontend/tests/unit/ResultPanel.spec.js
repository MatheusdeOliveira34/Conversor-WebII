import { shallowMount } from '@vue/test-utils'
import ResultPanel from '@/components/ResultPanel.vue'

const RESULT = {
  fileName: 'relatorio.pdf',
  contentType: 'application/pdf',
  sourceFormat: 'DOCX',
  targetFormat: 'PDF',
  sizeBytes: 48 * 1024,
  durationMs: 312
}

describe('ResultPanel.vue', () => {
  it('mostra sucesso com nome, tamanho e duração e emite download', async () => {
    const wrapper = shallowMount(ResultPanel, { propsData: { result: RESULT } })

    expect(wrapper.classes()).toContain('result-panel--success')
    expect(wrapper.text()).toContain('Conversão concluída.')
    expect(wrapper.find('.result-panel__detail').text()).toBe('relatorio.pdf · 48 KB · 312 ms')

    await wrapper.find('.btn--outline').trigger('click')
    expect(wrapper.emitted('download')).toHaveLength(1)
  })

  it('mostra a mensagem do ErrorResponse e emite retry', async () => {
    const message = 'O arquivo está corrompido ou não pôde ser lido.'
    const wrapper = shallowMount(ResultPanel, { propsData: { error: message } })

    expect(wrapper.classes()).toContain('result-panel--error')
    expect(wrapper.find('.result-panel__detail').text()).toBe(message)

    const button = wrapper.find('.btn--outline')
    expect(button.text()).toBe('Tentar novamente')
    await button.trigger('click')
    expect(wrapper.emitted('retry')).toHaveLength(1)
  })

  it('alertas têm ícone além da cor', () => {
    const success = shallowMount(ResultPanel, { propsData: { result: RESULT } })
    const failure = shallowMount(ResultPanel, { propsData: { error: 'Falhou' } })

    expect(success.find('.result-panel__icon').text()).toBe('✓')
    expect(failure.find('.result-panel__icon').text()).toBe('!')
  })

  it.each([
    ['sucesso', { result: RESULT }],
    ['erro', { error: 'Falhou' }]
  ])('"Nova conversão" emite reset no estado de %s', async (_, propsData) => {
    const wrapper = shallowMount(ResultPanel, { propsData })

    const reset = wrapper.find('.result-panel__reset')
    expect(reset.text()).toBe('Nova conversão')
    await reset.trigger('click')
    expect(wrapper.emitted('reset')).toHaveLength(1)
  })

  it('desabilita os botões quando disabled', () => {
    const wrapper = shallowMount(ResultPanel, { propsData: { result: RESULT, disabled: true } })

    wrapper.findAll('button').wrappers.forEach(button => {
      expect(button.attributes('disabled')).toBeDefined()
    })
  })
})
