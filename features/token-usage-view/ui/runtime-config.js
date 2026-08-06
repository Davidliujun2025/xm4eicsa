(function configureNavigation(global) {
    const local = global.location.hostname === '127.0.0.1' || global.location.hostname === 'localhost';
    const localUrl = (port) => `${global.location.protocol}//${global.location.hostname}:${port}/`;
    const defaults = local
        ? {
            workbenchUrl: localUrl(15174),
            favoriteScriptUrl: localUrl(15278),
            tokenUsageUrl: localUrl(15280)
        }
        : {
            workbenchUrl: `${global.location.origin}/workbench/`,
            favoriteScriptUrl: `${global.location.origin}/favorite-script-library/`,
            tokenUsageUrl: `${global.location.origin}/token-usage/`
        };

    global.__APP_NAV_CONFIG__ = {
        ...defaults,
        ...(global.__APP_NAV_CONFIG__ || {})
    };
})(window);
