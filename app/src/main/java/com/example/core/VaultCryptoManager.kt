package com.example.core

import android.content.Context
import java.io.File
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object VaultCryptoManager {
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val IV_SIZE_BYTES = 12
    private const val SALT = "FolioOfflineVaultSalt2026"

    private fun deriveKey(pin: String): SecretKey {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(pin.toCharArray(), SALT.toByteArray(Charsets.UTF_8), 10000, 256)
        val secret = factory.generateSecret(spec)
        return SecretKeySpec(secret.encoded, "AES")
    }

    fun encryptFileToVault(context: Context, sourceFile: File, pin: String): File {
        val vaultDir = File(context.filesDir, "private_vault").apply { mkdirs() }
        val destFile = File(vaultDir, "${sourceFile.nameWithoutExtension}_${System.currentTimeMillis()}.vlt")
        val plainBytes = sourceFile.readBytes()
        val iv = ByteArray(IV_SIZE_BYTES).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, deriveKey(pin), GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        val cipherBytes = cipher.doFinal(plainBytes)

        destFile.outputStream().use { out ->
            out.write(iv)
            out.write(cipherBytes)
        }
        return destFile
    }

    fun decryptVaultFileToTemp(context: Context, vaultFile: File, originalExt: String, pin: String): File {
        val tempDir = File(context.cacheDir, "vault_unlocked").apply { mkdirs() }
        val outTemp = File(tempDir, "${vaultFile.nameWithoutExtension}.$originalExt")
        val allBytes = vaultFile.readBytes()
        if (allBytes.size <= IV_SIZE_BYTES) return vaultFile

        val iv = allBytes.copyOfRange(0, IV_SIZE_BYTES)
        val cipherBytes = allBytes.copyOfRange(IV_SIZE_BYTES, allBytes.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, deriveKey(pin), GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        val plainBytes = cipher.doFinal(cipherBytes)
        outTemp.writeBytes(plainBytes)
        return outTemp
    }

    fun generatePkcs12SignatureDigest(file: File, signerName: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        if (file.exists()) {
            digest.update(file.readBytes())
        }
        digest.update(signerName.toByteArray(Charsets.UTF_8))
        val hashBytes = digest.digest()
        return hashBytes.take(12).joinToString("") { "%02X".format(it) }
    }

    data class StorageBreakdown(
        val libraryBytes: Long,
        val outputBytes: Long,
        val vaultBytes: Long,
        val cacheAndTempBytes: Long
    ) {
        val totalBytes: Long get() = libraryBytes + outputBytes + vaultBytes + cacheAndTempBytes
    }

    fun calculateStorageBreakdown(context: Context): StorageBreakdown {
        val libraryDir = File(context.filesDir, "library")
        val outputsDir = File(context.filesDir, "outputs")
        val vaultDir = File(context.filesDir, "private_vault")
        val cacheBytes = dirSize(context.cacheDir)
        return StorageBreakdown(
            libraryBytes = dirSize(libraryDir),
            outputBytes = dirSize(outputsDir),
            vaultBytes = dirSize(vaultDir),
            cacheAndTempBytes = cacheBytes
        )
    }

    fun clearCacheAndTempFiles(context: Context): Long {
        val before = dirSize(context.cacheDir)
        context.cacheDir.listFiles()?.forEach { child ->
            child.deleteRecursively()
        }
        return before
    }

    private fun dirSize(dir: File?): Long {
        if (dir == null || !dir.exists()) return 0L
        var total = 0L
        dir.walkTopDown().forEach { file ->
            if (file.isFile) total += file.length()
        }
        return total
    }
}
