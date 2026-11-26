<template>
  <div class="app">
    <main class="app-card">
      <header class="app-header">
        <h1 class="app-title">FileConverter</h1>
        <p class="app-subtitle">PDF • Word • Excel</p>
      </header>

      <section class="app-content" aria-label="Conversão de arquivo">
        <p class="app-hint">
          Envie um arquivo PDF, DOCX ou XLSX de até 10 MB e escolha o formato de destino.
        </p>
      </section>
    </main>
  </div>
</template>

<script>
export default {
  name: 'App',

  data () {
    return {
      file: null,
      targetFormat: null,
      conversions: [],
      loading: false,
      result: null,
      error: null
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

.app-hint {
  font-size: var(--font-size-help);
  color: var(--color-text-muted);
}
</style>
