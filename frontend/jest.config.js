module.exports = {
  preset: '@vue/cli-plugin-unit-jest',
  testMatch: ['<rootDir>/tests/unit/**/*.spec.js'],
  moduleNameMapper: {
    // Axios 1.x publica ESM para o navegador; no Jest usamos o build CommonJS
    '^axios$': 'axios/dist/node/axios.cjs'
  }
}
