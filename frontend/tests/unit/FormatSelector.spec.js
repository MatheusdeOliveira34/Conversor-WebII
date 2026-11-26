import { mount } from '@vue/test-utils'
import FormatSelector from '@/components/FormatSelector.vue'
import { targetFormatsFor } from '@/utils/fileFormat'

const CONVERSIONS = [
  { sourceFormat: 'PDF', targetFormats: ['DOCX', 'XLSX'] },
  { sourceFormat: 'DOCX', targetFormats: ['PDF', 'XLSX'] },
  { sourceFormat: 'XLSX', targetFormats: ['PDF', 'DOCX'] }
]

describe('FormatSelector.vue', () => {
  it('CT13: com origem DOCX mostra só PDF e Excel', () => {
    const wrapper = mount(FormatSelector, {
      propsData: { options: targetFormatsFor(CONVERSIONS, 'DOCX') }
    })

    const labels = wrapper.findAll('.format-selector__label').wrappers.map(w => w.text())
    expect(labels).toEqual(['PDF', 'Excel'])
    expect(wrapper.text()).not.toContain('Word')
  })

  it('emite input com o formato escolhido (v-model)', async () => {
    const wrapper = mount(FormatSelector, {
      propsData: { options: ['PDF', 'XLSX'], value: null }
    })

    await wrapper.findAll('input[type="radio"]').at(1).trigger('change')

    expect(wrapper.emitted('input')[0]).toEqual(['XLSX'])
  })

  it('marca a opção correspondente ao value', () => {
    const wrapper = mount(FormatSelector, {
      propsData: { options: ['PDF', 'XLSX'], value: 'PDF' }
    })

    const radios = wrapper.findAll('input[type="radio"]')
    expect(radios.at(0).element.checked).toBe(true)
    expect(radios.at(1).element.checked).toBe(false)
    expect(wrapper.find('.format-selector__option--selected').text()).toBe('PDF')
  })

  it('tem rótulo "Converter para" e pode ser desabilitado', () => {
    const wrapper = mount(FormatSelector, {
      propsData: { options: ['PDF'], disabled: true }
    })

    expect(wrapper.find('legend').text()).toBe('Converter para')
    expect(wrapper.find('fieldset').attributes('disabled')).toBeDefined()
  })
})

describe('targetFormatsFor', () => {
  it('devolve os destinos da origem e nunca a própria origem', () => {
    expect(targetFormatsFor(CONVERSIONS, 'PDF')).toEqual(['DOCX', 'XLSX'])
    expect(targetFormatsFor([{ sourceFormat: 'PDF', targetFormats: ['PDF', 'DOCX'] }], 'PDF')).toEqual(['DOCX'])
    expect(targetFormatsFor(CONVERSIONS, null)).toEqual([])
    expect(targetFormatsFor([], 'DOCX')).toEqual([])
  })
})
