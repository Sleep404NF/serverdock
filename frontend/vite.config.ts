import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    // /api 요청은 백엔드(8080)로 넘긴다. 같은 출처처럼 보여서 CORS 설정이 필요 없다.
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
