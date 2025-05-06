package ua.pp.lumivoid

import io.javalin.Javalin
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.connection.channel.direct.Session
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import org.slf4j.LoggerFactory
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

fun main(args: Array<String>) {
    val logger = LoggerFactory.getLogger("Main")

    val targetMac: String = System.getenv("TARGET_MAC")
    val targetIp: String = System.getenv("TARGET_IP")
    val serverIp: String = System.getenv("SERVER_IP")
    val serverPort: Int = System.getenv("SERVER_PORT").toInt()
    val sshUser: String = System.getenv("SSH_USER")
    val sshPassword: String = System.getenv("SSH_PASSWORD")


    val app = Javalin.create()

    app.get("/wol") {
        logger.info("WOL to $targetMac $targetIp")
        magicPacket(targetMac, targetIp)
        it.result("Sent")
    }

    app.get("/status") {
        logger.info("Status")
        val isPowered = InetAddress.getByName(targetIp).isReachable(100)
        if (isPowered) {
            it.result("Powered")
        } else {
            it.result("Disconnected")
        }
    }

    app.get("/shutdown") {
        logger.info("Shutting down")
        val ssh = SSHClient()
        try {
            ssh.addHostKeyVerifier(PromiscuousVerifier())
            ssh.connect(targetIp)
            ssh.authPassword(sshUser, sshPassword)

            val session: Session = ssh.startSession()
            session.use {
                val cmd = it.exec("sudo shutdown")
            }

            it.result("Success")
        } catch(_: Exception) {
            it.result("Failed")
        } finally {
            ssh.disconnect()
        }
    }

    app.start(serverIp, serverPort)
}

fun magicPacket(mac: String, ip: String = "255.255.255.255", port: Int = 9) {
    val packet = buildPacket(mac)
    val datagramSocket = DatagramSocket()
    try {
        datagramSocket.send(
            DatagramPacket(
                packet,
                packet.size,
                InetAddress.getByName(ip),
                port
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