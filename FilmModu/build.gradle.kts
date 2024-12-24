version = 4

cloudstream {
    authors     = listOf("keyiflerolsun")
    language    = "tr"
    description = "Film modun geldiyse yüksek kalitede en yeni filmleri izle, 1080p izleyebileceğiniz reklamsiz film sitesi."

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
    **/
    status  = 1 // will be 3 if unspecified
    tvTypes = listOf("Movie")
    iconUrl = "https://www.google.com/s2/favicons?domain=www.filmmodu17.com&sz=%size%"
}