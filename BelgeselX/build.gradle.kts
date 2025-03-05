version = 5

cloudstream {
    authors     = listOf("keyiflerolsun", "JustRelaxable")
    language    = "tr"
    description = "En yeni belgeseller, türkçe altyazılı yada dublaj olarak 1080p kalitesinde HD belgesel izle."

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
    **/
    status  = 1 // will be 3 if unspecified
    tvTypes = listOf("Documentary")
    iconUrl = "https://www.google.com/s2/favicons?domain=belgeselx.com&sz=%size%"
}