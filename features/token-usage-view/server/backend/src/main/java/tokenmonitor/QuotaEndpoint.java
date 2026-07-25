package tokenmonitor;


import java.net.URI;

record QuotaEndpoint(
        Provider provider,
        URI uri,
        String apiKey,
        QuotaResponseMapper mapper) {
}
