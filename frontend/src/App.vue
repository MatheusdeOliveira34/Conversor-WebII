<template>
  <div class="app">
    <main class="app-card">
      <header class="app-header">
        <h1 class="app-title">FileConverter</h1>
        <p class="app-subtitle">PDF • Word • Excel</p>
      </header>

      <section class="app-content" aria-label="Conversão de arquivo" :aria-busy="loading ? 'true' : 'false'">
        <file-dropzone
          v-if="!file"
          :loading="loadingFormats"
          :disabled="Boolean(formatsError)"
          @file-selected="onFileSelected"
          @error="onFileError"
        />

        <template v-if="file">
          <div class="file-card">
            <span class="format-tag" :class="`format-tag--${sourceFormat.toLowerCase()}`">
              {{ sourceFormat }}
            </span>
            <span class="file-card__name" :title="file.name">{{ file.name }}</span>
            <span class="file-card__size">{{ fileSize }}</span>
            <button
              type="button"
              class="file-card__remove"
              :disabled="loading"
              :aria-label="`Remover ${file.name}`"
              @click="reset"
            >
              Remover
            </button>
          </div>

          <template v-if="targetOptions.length">
            <div class="app-target">
              <format-selector
                :value="targetFormat"
                :options="targetOptions"
                :disabled="loading"
                @input="onTargetSelected"
              />
              <p class="app-notice">
                A conversão leva texto e tabelas; imagens e formatação podem se perder.
              </p>
            </div>

            <button
              v-if="!result && !error"
              type="button"
              class="btn btn--primary"
              :disabled="!canConvert"
              @click="submit"
            >
              <span v-if="loading" class="spinner" aria-hidden="true" />
              {{ loading ? 'Convertendo…' : 'Converter' }}
            </button>
          </template>
        </template>

        <!-- Região anunciada por leitores de tela: progresso, sucesso e erros -->
        <div class="app-status" aria-live="polite">
          <span v-if="loading" class="sr-only">Convertendo o arquivo, aguarde.</span>

          <p v-if="formatsError" class="app-alert app-alert--error">
            <span class="app-alert__icon" aria-hidden="true">!</span>
            {{ formatsError }}
          </p>

          <p v-if="file && !targetOptions.length" class="app-alert app-alert--error">
            <span class="app-alert__icon" aria-hidden="true">!</span>
            Conversão não suportada para este arquivo.
          </p>

          <result-panel
            v-if="result || error"
            :result="result"
            :error="error"
            :disabled="loading"
            @download="download"
            @retry="submit"
            @reset="reset"
          />
        </div>
      </section>
    </main>
  </div>
</template>

<script>
import FileDropzone from '@/components/FileDropzone.vue'
import FormatSelector from '@/components/FormatSelector.vue'
import ResultPanel from '@/components/ResultPanel.vue'
import { convert, errorMessage, getFormats } from '@/services/conversionApi'
import { createDownloadUrl, revokeDownloadUrl, triggerDownload } from '@/utils/download'
import { formatFileSize, formatFromFileName, targetFormatsFor } from '@/utils/fileFormat'

export const FORMATS_ERROR = 'Não foi possível carregar os formatos. Recarregue a página.'

export default {
  name: 'App',

  components: { FileDropzone, FormatSelector, ResultPanel },

  data () {
    return {
      file: null,
      targetFormat: null,
      conversions: [],
      loadingFormats: true,
      formatsError: null,
      loading: false,
      result: null,
      error: null,
      downloadUrl: null
    }
  },

  computed: {
    sourceFormat () {
      return this.file ? formatFromFileName(this.file.name) : null
    },
    fileSize () {
      return this.file ? formatFileSize(this.file.size) : ''
    },
    targetOptions () {
      return targetFormatsFor(this.conversions, this.sourceFormat)
    },
    canConvert () {
      return Boolean(this.file && this.targetFormat) && !this.loading
    }
  },

  created () {
    this.loadFormats()
  },

  beforeDestroy () {
    revokeDownloadUrl(this.downloadUrl)
  },

  methods: {
    // UC02: a tela pede as conversões suportadas ao abrir
    async loadFormats () {
      this.loadingFormats = true
      this.formatsError = null
      try {
        this.conversions = await getFormats()
      } catch (e) {
        this.conversions = []
        this.formatsError = FORMATS_ERROR
      } finally {
        this.loadingFormats = false
      }
    },

    onFileSelected (file) {
      // UC01 A3: trocar o arquivo limpa o destino e o resultado anteriores
      this.file = file
      this.targetFormat = null
      this.clearResult()
    },

    onFileError () {
      // UC01 A1/A2: arquivo rejeitado não fica selecionado
      this.file = null
      this.targetFormat = null
    },

    onTargetSelected (format) {
      // Outro destino pede nova conversão: some o resultado anterior
      this.targetFormat = format
      this.clearResult()
    },

    // UC01, passos 6–8: envia, gera o download e mostra o resultado
    async submit () {
      if (!this.canConvert) {
        return
      }
      this.loading = true
      this.clearResult()
      try {
        const { contentBase64, ...metadata } = await convert(this.file, this.targetFormat)
        this.downloadUrl = createDownloadUrl(contentBase64, metadata.contentType)
        this.result = metadata
        this.download()
      } catch (e) {
        this.error = errorMessage(e)
      } finally {
        this.loading = false
      }
    },

    download () {
      if (this.downloadUrl && this.result) {
        triggerDownload(this.downloadUrl, this.result.fileName)
      }
    },

    // UC03: "Nova conversão" ou "Remover" voltam ao estado inicial
    reset () {
      if (this.loading) {
        return // UC03 A1: bloqueado enquanto a conversão está em andamento
      }
      this.file = null
      this.targetFormat = null
      this.clearResult()
    },

    /** Limpa resultado e erro e libera a URL do download anterior (URL.revokeObjectURL). */
    clearResult () {
      revokeDownloadUrl(this.downloadUrl)
      this.downloadUrl = null
      this.result = null
      this.error = null
    }
  }
}
</script>

<style scoped>
.app {
  display: flex;
  justify-content: center;
  align-items: flex-start;
  min-height: 100vh;
  padding: var(--space-5) var(--space-2);
}

.app-card {
  width: 100%;
  max-width: var(--card-max-width);
  padding: var(--space-4);
  background-color: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  box-shadow: var(--shadow-card);
}

.app-header {
  margin-bottom: var(--space-3);
}

.app-title {
  font-size: var(--font-size-title);
  font-weight: 700;
  line-height: 1.2;
}

.app-subtitle {
  margin-top: var(--space-1);
  font-size: var(--font-size-body);
  font-weight: 400;
  color: var(--color-text-muted);
}

.app-content {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

/* Indicador de progresso no botão "Converter" (RF08) */
.spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.4);
  border-top-color: #ffffff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@media (prefers-reduced-motion: reduce) {
  .spinner {
    animation-duration: 2.4s;
  }
}

.app-target {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

/* Aviso fixo de fidelidade (interface.md, seção 4) */
.app-notice {
  font-size: var(--font-size-help);
  color: var(--color-text-muted);
}

.app-status:empty {
  display: none;
}

.app-alert {
  display: flex;
  align-items: center;
  gap: var(--space-1);
  padding: var(--space-1) var(--space-2);
  border-radius: var(--radius);
  font-size: var(--font-size-label);
}

.app-alert__icon {
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

.app-alert--error {
  color: var(--color-error);
  background-color: #fef2f2;
  border: 1px solid #fecaca;
}

/* Cartão do arquivo selecionado (RF07): formato, nome e tamanho */
.file-card {
  display: flex;
  align-items: center;
  gap: var(--space-1);
  padding: var(--space-2);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
}

.file-card__name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 500;
}

.file-card__size {
  font-size: var(--font-size-help);
  color: var(--color-text-muted);
  white-space: nowrap;
}

/* Ação secundária como link (interface.md, seção 4) */
.file-card__remove {
  padding: 4px;
  border: 0;
  border-radius: 4px;
  background: none;
  color: var(--color-primary);
  font-size: var(--font-size-label);
  font-weight: 500;
  text-decoration: underline;
}

.file-card__remove:hover:not(:disabled) {
  color: var(--color-primary-hover);
}

.file-card__remove:disabled {
  color: var(--color-text-muted);
  text-decoration: none;
}

/* Etiqueta de formato: cor + texto, nunca só a cor */
.format-tag {
  display: inline-block;
  padding: 2px var(--space-1);
  border-radius: 4px;
  font-size: var(--font-size-help);
  font-weight: 700;
  color: #ffffff;
  letter-spacing: 0.02em;
}

.format-tag--pdf {
  background-color: var(--color-pdf);
}

.format-tag--docx {
  background-color: var(--color-docx);
}

.format-tag--xlsx {
  background-color: var(--color-xlsx);
}

/* Responsivo (RNF09): abaixo de 600 px o cartão ocupa a largura com margem de 16 px */
@media (max-width: 599px) {
  .app {
    padding: var(--space-2);
  }

  .app-card {
    max-width: none;
    padding: var(--space-3) var(--space-2);
  }

  .file-card {
    flex-wrap: wrap;
  }

  .file-card__name {
    flex-basis: calc(100% - 64px);
  }
}
</style>
