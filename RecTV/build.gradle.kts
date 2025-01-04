version = 36

cloudstream {
    authors     = listOf("keyiflerolsun", "yusiqo")
    language    = "tr"
    description = "RecTv APK, Türkiye’deki en popüler Çevrimiçi Medya Akış platformlarından biridir. Filmlerin, Canlı Sporların, Web Dizilerinin ve çok daha fazlasının keyfini ücretsiz çıkarın."

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
    **/
    status  = 1 // will be 3 if unspecified
    tvTypes = listOf("Movie", "Live", "TvSeries")
    iconUrl = "https://rectvapk.cc/wp-content/uploads/2023/02/Rec-TV.webp"
}