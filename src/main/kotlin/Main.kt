package ua.pp.lumivoid

import io.javalin.Javalin
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.connection.channel.direct.Session
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
fun main() {
    val app = Javalin.create()

    app.get("/wol") {
        magicPacket("")
        it.result("Hello World")
    }

    app.get("/status") {
        val isPowered = InetAddress.getByName("").isReachable(100)
        if (isPowered) {
            it.result("Powered")
        } else {
            it.result("Disconnected")
        }
    }

    app.get("/shutdown") {
        val ssh = SSHClient()
        try {
            ssh.addHostKeyVerifier(PromiscuousVerifier())
            ssh.connect("")
            ssh.authPassword("", "")

            val session: Session = ssh.startSession()
            session.use {
                val cmd = it.exec("sudo shutdown")
                println(cmd.inputStream.bufferedReader().readText())
            }

            it.result("Success")
        } catch(_: Exception) {
            it.result("Failed")
        } finally {
            ssh.disconnect()
        }
    }

    app.start(8080)
}

fun magicPacket(mac: String, ip: String = "255.255.255.255", PORT: Int = 9) {
    val packet = buildPacket(mac)
    val datagramSocket = DatagramSocket()
    try {
        datagramSocket.send(
            DatagramPacket(
                packet,
                packet.size,
                InetAddress.getByName(ip),
                PORT
            )
        )
        datagramSocket.close()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun buildPacket(mac: String): ByteArray {
    // prepare packet   (6 bytes header) (16 * 6 bytes MAC)
    val temp = ByteArray(6 + 6 * 16)
    val macBytes = ByteArray(6)
    for (i in 0..5) {
        temp[i] = 0xFF.toByte()
        macBytes[i] = mac.split(":", "-")[i].toInt(16).toByte()
    }
    for (i in 6 until temp.size step macBytes.size) System.arraycopy(
        macBytes,
        0,
        temp,
        i,
        macBytes.size
    )
    return temp
}