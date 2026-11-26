<template>
  <div class="file-dropzone">
    <input
      :id="inputId"
      ref="input"
      type="file"
      class="file-dropzone__input"
      :accept="accept"
      :disabled="isDisabled"
      @change="onChange"
    >
    <label
      :for="inputId"
      class="file-dropzone__area"
      :class="{
        'file-dropzone__area--dragging': dragging,
        'file-dropzone__area--disabled': isDisabled
      }"
      @dragenter.prevent="onDragEnter"
      @dragover.prevent="onDragEnter"
      @dragleave.prevent="onDragLeave"
      @drop.prevent="onDrop"
    >
      <span v-if="loading" class="file-dropzone__title">Carregando formatos…</span>
      <template v-else>
        <span class="file-dropzone__title">Arraste o arquivo aqui</span>
        <span class="file-dropzone__subtitle">ou clique para escolher</span>
        <span class="file-dropzone__help">PDF, DOCX ou XLSX até 10 MB</span>
      </template>
    </label>

    <!-- Sempre no DOM para o leitor de tela anunciar a mensagem quando ela aparecer -->
    <div class="file-dropzone__status" aria-live="polite">
      <p v-if="errorMessage" class="file-dropzone__error">
        <span class="file-dropzone__error-icon" aria-hidden="true">!</span>
        {{ errorMessage }}
      </p>
    </div>
  </div>
</template>

<script>
import { ACCEPTED_EXTENSIONS, validateFile } from '@/utils/fileFormat'

/**
 * Área de upload por clique ou arrastar e soltar (RF01).
 * Valida extensão e tamanho (RF02) e emite:
 *  - `file-selected` (file, format) quando o arquivo é aceito;
 *  - `error` (message) quando é rejeitado.
 */
export default {
  name: 'FileDropzone',

  props: {
    disabled: {
      type: Boolean,
      default: false
    },
    loading: {
      type: Boolean,
      default: false
    }
  },

  data () {
    return {
      dragging: false,
      errorMessage: null
    }
  },

  computed: {
    inputId () {
      return `file-dropzone-input-${this._uid}`
    },
    accept () {
      return ACCEPTED_EXTENSIONS
    },
    isDisabled () {
      return this.disabled || this.loading
    }
  },

  methods: {
    onChange (event) {
      const files = event.target.files
      this.handleFile(files && files[0])
      // Permite escolher o mesmo arquivo de novo depois de um erro
      event.target.value = ''
    },

    onDragEnter () {
      if (!this.isDisabled) {
        this.dragging = true
      }
    },

    onDragLeave () {
      this.dragging = false
    },

    onDrop (event) {
      this.dragging = false
      if (this.isDisabled) {
        return
      }
      const files = event.dataTransfer && event.dataTransfer.files
      this.handleFile(files && files[0])
    },

    handleFile (file) {
      if (!file) {
        return
      }
      const { format, error } = validateFile(file)
      if (error) {
        this.errorMessage = error
        this.$emit('error', error)
        return
      }
      this.errorMessage = null
      this.$emit('file-selected', file, format)
    }
  }
}
</script>

<style scoped>
.file-dropzone__input {
  /* Escondido visualmente, mas ainda alcançável pelo teclado e leitores de tela */
  position: absolute;
  width: 1px;
  height: 1px;
  overflow: hidden;
  clip: rect(0 0 0 0);
  white-space: nowrap;
}

.file-dropzone__area {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: var(--space-4) var(--space-2);
  background-color: var(--color-surface);
  border: 2px dashed var(--color-border);
  border-radius: var(--radius);
  text-align: center;
  cursor: pointer;
  transition: border-color 0.15s, background-color 0.15s;
}

.file-dropzone__area:hover,
.file-dropzone__area--dragging {
  border-color: var(--color-primary);
  background-color: #eff6ff;
}

.file-dropzone__input:focus-visible + .file-dropzone__area {
  /* Foco do teclado no input escondido aparece na área de upload */
  outline: 2px solid var(--color-primary);
  outline-offset: 2px;
  border-color: var(--color-primary);
}

.file-dropzone__area--disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.file-dropzone__area--disabled:hover {
  border-color: var(--color-border);
  background-color: var(--color-surface);
}

.file-dropzone__title {
  font-weight: 600;
}

.file-dropzone__subtitle {
  color: var(--color-text-muted);
}

.file-dropzone__help {
  margin-top: var(--space-1);
  font-size: var(--font-size-help);
  color: var(--color-text-muted);
}

.file-dropzone__error {
  display: flex;
  align-items: center;
  gap: var(--space-1);
  margin-top: var(--space-1);
  font-size: var(--font-size-label);
  color: var(--color-error);
}

.file-dropzone__error-icon {
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background-color: var(--color-error);
  color: #ffffff;
  font-size: var(--font-size-help);
  font-weight: 700;
}
</style>
