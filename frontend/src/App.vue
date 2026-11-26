<template>
  <div class="app">
    <main class="app-card">
      <header class="app-header">
        <h1 class="app-title">FileConverter</h1>
        <p class="app-subtitle">PDF • Word • Excel</p>
      </header>

      <section class="app-content" aria-label="Conversão de arquivo">
        <file-dropzone
          v-if="!file"
          @file-selected="onFileSelected"
          @error="onFileError"
        />

        <div v-else class="file-card">
          <span class="format-tag" :class="`format-tag--${sourceFormat.toLowerCase()}`">
            {{ sourceFormat }}
          </span>
          <span class="file-card__name" :title="file.name">{{ file.name }}</span>
          <span class="file-card__size">{{ fileSize }}</span>
        </div>
      </section>
    </main>
  </div>
</template>

<script>
import FileDropzone from '@/components/FileDropzone.vue'
import { formatFileSize, formatFromFileName } from '@/utils/fileFormat'

export default {
  name: 'App',

  components: { FileDropzone },

  data () {
    return {
      file: null,
      targetFormat: null,
      conversions: [],
      loading: false,
      result: null,
      error: null
    }
  },

  computed: {
    sourceFormat () {
      return this.file ? formatFromFileName(this.file.name) : null
    },
    fileSize () {
      return this.file ? formatFileSize(this.file.size) : ''
    }
  },

  methods: {
    onFileSelected (file) {
      // UC01 A3: trocar o arquivo limpa o destino e o resultado anteriores
      this.file = file
      this.targetFormat = null
      this.result = null
      this.error = null
    },

    onFileError () {
      // UC01 A1/A2: arquivo rejeitado não fica selecionado
      this.file = null
      this.targetFormat = null
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
</style>
