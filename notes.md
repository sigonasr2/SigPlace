User Database
=============
Storage using flat files. User information stores the password hash. Store a session token too. Avatar uploader.


    Sample multipart/form-data upload:
    
        User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/101.0.4951.54 Safari/537.36
        Content-Length: 12360
        Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9
        Accept-Encoding: gzip, deflate, br
        Accept-Language: en-US,en;q=0.9
        Cache-Control: max-age=0
        Content-Type: multipart/form-data; boundary=----WebKitFormBoundarywqXMKIHZy5A6bMPX
        Cookie: ajs_anonymous_id=1999b481-c5ea-4e5f-8f14-3b6cd2b48fe1;gitpod-marketing-website-visited=true;_gcl_au=1.1.219623143.1650911648;_sc_token=v2%3AW5B7n8Dm9KQZEXMabHwK9nMKhgLjrPjD0pQqHJMQt5cx-Gha8jgih1VLumJsg0QuLuanOSaoizWUoyeEgq_ErPSHc9xoH8xn81hnLlD8-rs5bNK9z7vIvm2CTma-S3ahSUWYVm5tkBTA72TrmhywbQI33CKLTwuKoeAfL0vFjBLG3jwWcLCva8h0Ecg8oTBl
        Origin: 
        Referer: 
        Sec-Ch-Ua: " Not A;Brand";v="99", "Chromium";v="101", "Google Chrome";v="101"
        Sec-Ch-Ua-Mobile: ?0
        Sec-Ch-Ua-Platform: "Windows"
        Sec-Fetch-Dest: document
        Sec-Fetch-Mode: navigate
        Sec-Fetch-Site: same-origin
        Sec-Fetch-User: ?1
        Upgrade-Insecure-Requests: 1
        X-Forwarded-For: 137.242.111.127
        X-Forwarded-Host: 
        X-Forwarded-Proto: https

        ------WebKitFormBoundarywqXMKIHZy5A6bMPX
        Content-Disposition: form-data; name="file1"; filename="Love_Live!_Logo.png"
        Content-Type: image/png
        <...>

        ------WebKitFormBoundarywqXMKIHZy5A6bMPX--