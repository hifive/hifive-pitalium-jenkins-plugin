//uriencorder.js
//
//画像ファイルへのリンクのパスをエンコードするスクリプト
//a, imgタグのアドレスの#のみエンコード
//


//TODO タグの取得をid="attachments"以下に限定する
function uriencorder(){
var element = document.getElementsByTagName("a");
for (var i = 0; i < element.length; i++) {
    var addr = element[i].getAttribute("href");
    if (addr!=null){
    var addr = addr.replace(/#/g, "%23");
    //addr=encodeURIComponent(addr);
    console.log(addr);
    element[i].setAttribute("href",addr);
    }
}
var element = document.getElementsByTagName("img");
for (var i = 0; i < element.length; i++) {
    var addr = element[i].getAttribute("src");
    if (addr!=null){
    var addr = addr.replace(/#/g, "%23");
    //addr=encodeURIComponent(addr);
    console.log(addr);
    element[i].setAttribute("src",addr);
    }
}
}