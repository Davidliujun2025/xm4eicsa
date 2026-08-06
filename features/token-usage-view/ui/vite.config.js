const jsonForInlineScript = (value) => JSON.stringify(value).replace(/</g, '\\u003c');

export default {
  plugins: [
    {
      name: 'token-usage-runtime-navigation',
      transformIndexHtml() {
        const navigation = {
          workbenchUrl: process.env.VITE_WORKBENCH_URL,
          favoriteScriptUrl: process.env.VITE_FAVORITE_SCRIPT_URL,
          tokenUsageUrl: process.env.VITE_TOKEN_USAGE_URL,
        };
        const configuredNavigation = Object.fromEntries(
          Object.entries(navigation).filter(([, value]) => Boolean(value)),
        );

        if (Object.keys(configuredNavigation).length === 0) {
          return [];
        }

        return [
          {
            tag: 'script',
            children: `window.__APP_NAV_CONFIG__ = ${jsonForInlineScript(configuredNavigation)};`,
            injectTo: 'head-prepend',
          },
        ];
      },
    },
  ],
  server: {
    proxy: {
      '/api': {
        target: process.env.VITE_API_PROXY_TARGET || 'http://127.0.0.1:18080',
        changeOrigin: true,
      },
    },
  },
};
