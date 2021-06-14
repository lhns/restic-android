package de.lolhens.resticui

import android.content.pm.PackageManager
import android.net.DnsResolver
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.material.snackbar.Snackbar
import de.lolhens.resticui.databinding.ActivityMainBinding
import java.io.File
import java.net.InetAddress
import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    private fun checkPermission(permission: String, requestCode: Int): Boolean {
        if (ContextCompat.checkSelfPermission(this@MainActivity, permission) == PackageManager.PERMISSION_DENIED) {
            // Requesting the permission
            ActivityCompat.requestPermissions(this@MainActivity, arrayOf(permission), requestCode)
            return false
        } else {
            return true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        binding.fab.setOnClickListener { view ->

            println(applicationInfo.nativeLibraryDir)

            fun restic(cmd: String) {
                val resticBin = applicationInfo.nativeLibraryDir + "/restic"
                val prootBin = applicationInfo.nativeLibraryDir + "/proot"

                val s3Url = "https://"
                val uri = URI(s3Url)
                val domain: String = uri.getHost()
                val inetAddressFuture = CompletableFuture.supplyAsync {
                    InetAddress.getByName(domain)
                }

                val inetAddress = inetAddressFuture.get()

                val hostsFile = File.createTempFile("hosts", "", cacheDir);
                hostsFile.writeText(inetAddress.hostAddress + " " + domain, StandardCharsets.UTF_8)

                val p = Runtime.getRuntime().exec(
                    arrayOf(
                        prootBin,
                        "-b", hostsFile.absolutePath + ":/etc/hosts",
                        "-b", "/system:/system",
                        "-b", "/storage:/storage",
                        "-b", "/data:/data",
                        "--kill-on-exit",
                        resticBin,
                        "--json",
                        cmd,
                        "-r",
                        "s3:" + s3Url
                    ),
                    arrayOf(
                        "AWS_ACCESS_KEY_ID=",
                        "AWS_SECRET_ACCESS_KEY=",
                        "RESTIC_PASSWORD=",
                        "LD_LIBRARY_PATH=" + applicationInfo.nativeLibraryDir,
                        "PROOT_TMP_DIR=" + cacheDir,
                        "PROOT_LOADER=" + applicationInfo.nativeLibraryDir + "/loader",
                        "PROOT_LOADER_32=" + applicationInfo.nativeLibraryDir + "/loader32",
                        "PATH=/system/bin"
                    )
                )
                /*val p = Runtime.getRuntime().exec(
                    arrayOf(string)
                )*/

                val out = p.inputStream
                val err = p.errorStream
                val outBuff = out.bufferedReader()
                val errBuff = err.bufferedReader()
                while (true) {
                    var line = outBuff.readLine()
                    var str = "OUT"
                    if (line == null) {
                        line = errBuff.readLine()
                        str = "ERR"
                    }
                    if (line == null) break;
                    println(str + ": " + line)
                }

                //p.inputStream.bufferedReader().lineSequence().iterator().forEach { e -> System.out.println(e); }
                //p.errorStream.bufferedReader().lineSequence().iterator().forEach { e -> System.out.println(e); }
                p.waitFor()
            }

            restic("snapshots")

            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }
}