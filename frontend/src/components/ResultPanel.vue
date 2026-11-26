<template>
  <div
    class="result-panel"
    :class="isSuccess ? 'result-panel--success' : 'result-panel--error'"
  >
    <div class="result-panel__message">
      <span class="result-panel__icon" aria-hidden="true">{{ isSuccess ? '✓' : '!' }}</span>
      <div>
        <p class="result-panel__title">
          {{ isSuccess ? 'Conversão concluída.' : 'Não foi possível converter.' }}
        </p>
        <p v-if="isSuccess" class="result-panel__detail">
          {{ result.fileName }} · {{ size }} · {{ result.durationMs }} ms
        </p>
        <p v-else class="result-panel__detail">{{ error }}</p>
      </div>
    </div>

    <div class="result-panel__actions">
      <button
        v-if="isSuccess"
        type="button"
        class="btn btn--outline"
        :disabled="disabled"
        @click="$emit('download')"
      >
        Baixar novamente
      </button>
      <button
        v-else
        type="button"
        class="btn btn--outline"
        :disabled="disabled"
        @click="$emit('retry')"
      >
        Tentar novamente
      </button>
      <button
        type="button"
        class="result-panel__reset"
        :disabled="disabled"
        @click="$emit('reset')"
      >
        Nova conversão
      </button>
    </div>
  </div>
</template>

<script>
import { formatFileSize } from '@/utils/fileFormat'

/**
 * Resultado da conversão. Com `result` (ConversionResponse sem o base64) mostra sucesso
 * e emite `download`; com `error` (mensagem) mostra o erro e emite `retry`.
 * Nos dois casos "Nova conversão" emite `reset` (UC03).
 */
export default {
  name: 'ResultPanel',

  props: {
    result: {
      type: Object,
      default: null
    },
    error: {
      type: String,
      default: null
    },
    disabled: {
      type: Boolean,
      default: false
    }
  },

  computed: {
    isSuccess () {
      return Boolean(this.result) && !this.error
    },
    size () {
      return this.result ? formatFileSize(this.result.sizeBytes) : ''
    }
  }
}
</script>

<style scoped>
.result-panel {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  padding: var(--space-2);
  border: 1px solid;
  border-radius: var(--radius);
}

.result-panel--success {
  border-color: #bbf7d0;
  background-color: #f0fdf4;
}

.result-panel--error {
  border-color: #fecaca;
  background-color: #fef2f2;
}

.result-panel__message {
  display: flex;
  align-items: flex-start;
  gap: var(--space-1);
}

.result-panel__icon {
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  font-size: var(--font-size-label);
  font-weight: 700;
  color: #ffffff;
}

.result-panel--success .result-panel__icon {
  background-color: var(--color-success);
}

.result-panel--error .result-panel__icon {
  background-color: var(--color-error);
}

.result-panel__title {
  font-weight: 600;
}

.result-panel--success .result-panel__title {
  color: var(--color-success);
}

.result-panel--error .result-panel__title {
  color: var(--color-error);
}

.result-panel__detail {
  font-size: var(--font-size-label);
  color: var(--color-text);
  word-break: break-word;
}

.result-panel__actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-1);
}

/* Ação secundária como link (interface.md, seção 4) */
.result-panel__reset {
  padding: var(--space-1);
  border: 0;
  border-radius: 4px;
  background: none;
  color: var(--color-primary);
  font-weight: 500;
  text-decoration: underline;
}

.result-panel__reset:hover:not(:disabled) {
  color: var(--color-primary-hover);
}

@media (max-width: 599px) {
  .result-panel__actions {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
