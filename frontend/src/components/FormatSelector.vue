<template>
  <fieldset class="format-selector" :disabled="disabled">
    <legend class="format-selector__legend">Converter para</legend>

    <div class="format-selector__options">
      <label
        v-for="option in options"
        :key="option"
        class="format-selector__option"
        :class="{ 'format-selector__option--selected': option === value }"
      >
        <input
          type="radio"
          class="format-selector__radio"
          :name="groupName"
          :value="option"
          :checked="option === value"
          @change="$emit('input', option)"
        >
        <span class="format-selector__label">{{ labelFor(option) }}</span>
      </label>
    </div>
  </fieldset>
</template>

<script>
import { FORMAT_LABELS } from '@/utils/fileFormat'

/**
 * Escolha do formato de destino (RF04). Recebe só os destinos válidos em `options`
 * e emite `input`, então funciona com `v-model`.
 */
export default {
  name: 'FormatSelector',

  props: {
    options: {
      type: Array,
      required: true
    },
    value: {
      type: String,
      default: null
    },
    disabled: {
      type: Boolean,
      default: false
    }
  },

  computed: {
    groupName () {
      return `target-format-${this._uid}`
    }
  },

  methods: {
    labelFor (format) {
      return FORMAT_LABELS[format] || format
    }
  }
}
</script>

<style scoped>
.format-selector {
  margin: 0;
  padding: 0;
  border: 0;
  min-width: 0;
}

.format-selector__legend {
  margin-bottom: var(--space-1);
  padding: 0;
  font-size: var(--font-size-label);
  font-weight: 600;
}

.format-selector__options {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-1);
}

.format-selector__option {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 96px;
  padding: var(--space-1) var(--space-2);
  border: 1px solid var(--color-border);
  border-radius: 999px;
  font-weight: 500;
  cursor: pointer;
  transition: border-color 0.15s, background-color 0.15s, color 0.15s;
}

.format-selector__option:hover {
  border-color: var(--color-primary);
}

.format-selector__option--selected {
  border-color: var(--color-primary);
  background-color: var(--color-primary);
  color: #ffffff;
}

.format-selector__radio {
  /* O círculo nativo some; o cartão inteiro vira o controle */
  position: absolute;
  opacity: 0;
  width: 1px;
  height: 1px;
}

.format-selector[disabled] .format-selector__option {
  cursor: not-allowed;
  opacity: 0.6;
}
</style>
