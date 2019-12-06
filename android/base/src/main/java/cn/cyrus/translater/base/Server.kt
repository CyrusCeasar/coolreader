package cn.cyrus.translater.base

const val SERVER_IP = "103.91.67.151"
const val TEST_SERVER_IP="192.168.77.1:8000"

fun httpUrl():String{
    return "http://$SERVER_IP/"
}
fun httpsUrl():String{
    return "https://$SERVER_IP/"
}
