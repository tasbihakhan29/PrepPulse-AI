/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,jsx}"
  ],
  theme: {
    extend: {
      colors: {
        primary: '#4F46E5',
        success: '#10B981',
        background: '#F9FAFB'
      },
      spacing: {
        '128': '32rem'
      }
    }
  },
  plugins: []
}
