package cz.regulus.navrhtc

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import cz.regulus.navrhtc.AppState.TypVypoctu
import cz.regulus.navrhtc.Jednotky.kW.Companion.kW
import cz.regulus.navrhtc.Jednotky.kWh.Companion.kWh
import cz.regulus.navrhtc.ui.theme.NavrhTcTheme
import kotlinx.coroutines.tasks.await

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        setContent {
            NavrhTcTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    ScreenContent()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenContent() {
    var state by remember { mutableStateOf(AppState()) }
    LocalContext.current.also { ctx ->
        LaunchedEffect(Unit) {
            val sharedPref = ctx.getSharedPreferences("Prefs_TC_Cerpadla", Context.MODE_PRIVATE)
            val json = if (isOnline(ctx)) {
                val realtime = Firebase.database("https://navrhtc-default-rtdb.europe-west1.firebasedatabase.app")
                val reference = realtime.reference
                val value = reference.get().await().value
                Gson().toJson(value)
            } else {
                sharedPref.getString("cerpadla", "{}") ?: "{}"
            }
            sharedPref.edit {
                putString("cerpadla", json)
            }
            val cerpadla = Gson().fromJson(json, TepelnaCerpadla::class.java)
            state = state.copy(cerpadla = cerpadla)
        }
    }


    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("Dimenzování tepelných čerpadel")
                },
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues = paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .weight(1F),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Karticka {
                    TypVypoctu.typy.forEach { typPaliva ->
                        Card(
                            onClick = {
                                state = state.copy(typVypoctu = typPaliva)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(
                                    selected = state.typVypoctu::class == typPaliva::class,
                                    onClick = {
                                        state = state.copy(typVypoctu = typPaliva)
                                    }
                                )
                                Text(typPaliva.nazev)
                            }
                        }
                    }
                }

                when (state.typVypoctu) {
                    is TypVypoctu.Tuhy -> {
                        val vypocet = state.typVypoctu as TypVypoctu.Tuhy
                        Karticka {
                            Text("Uhlí", fontSize = 20.sp)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("Typ:")
                                Dropdown(
                                    values = TypVypoctu.Tuhy.TypUhli.values().toList(),
                                    display = { it.nazev },
                                    currentValue = vypocet.typUhli,
                                    onValueClicked = { vybrano ->
                                        state = state.copy(typVypoctu = vypocet.copy(typUhli = vybrano))
                                    }
                                )
                                OutlinedTextField(
                                    value = vypocet.uhliString,
                                    onValueChange = {
                                        state = state.copy(typVypoctu = vypocet.copy(uhliString = it.replace(',', '.')))
                                    },
                                    modifier = Modifier
                                        .weight(1F)
                                        .padding(horizontal = 4.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    trailingIcon = {
                                        Text("q")
                                    }
                                )
                            }
                        }
                        Karticka {
                            Text("Dřevo", fontSize = 20.sp)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("Typ:")
                                Dropdown(
                                    values = TypVypoctu.Tuhy.TypDreva.values().toList(),
                                    display = { it.nazev },
                                    currentValue = vypocet.typDreva,
                                    onValueClicked = { vybrano ->
                                        state = state.copy(typVypoctu = vypocet.copy(typDreva = vybrano))
                                    }
                                )
                                OutlinedTextField(
                                    value = vypocet.drevoString,
                                    onValueChange = {
                                        state = state.copy(typVypoctu = vypocet.copy(drevoString = it.replace(',', '.')))
                                    },
                                    modifier = Modifier
                                        .weight(1F)
                                        .padding(horizontal = 4.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    trailingIcon = {
                                        Text("m³")
                                    }
                                )
                            }
                        }
                        Karticka {
                            Text("Ostatní", fontSize = 20.sp)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("Typ:")
                                Dropdown(
                                    values = TypVypoctu.Tuhy.TypOstatniho.values().toList(),
                                    display = { it.nazev },
                                    currentValue = vypocet.typOstatniho,
                                    onValueClicked = { vybrano ->
                                        state = state.copy(typVypoctu = vypocet.copy(typOstatniho = vybrano))
                                    }
                                )
                                OutlinedTextField(
                                    value = vypocet.ostatniString,
                                    onValueChange = {
                                        state = state.copy(typVypoctu = vypocet.copy(ostatniString = it.replace(',', '.')))
                                    },
                                    modifier = Modifier
                                        .weight(1F)
                                        .padding(horizontal = 4.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    trailingIcon = {
                                        Text("kg")
                                    }
                                )
                            }
                        }

                        Karticka {
                            Text("Účinnost kotle:", fontSize = 20.sp, modifier = Modifier.padding(bottom = 8.dp))
                            Row(
                                verticalAlignment = Alignment.Bottom,
                            ) {
                                OutlinedTextField(
                                    value = vypocet.ucinnostKotleString,
                                    onValueChange = {
                                        state = state.copy(typVypoctu = vypocet.copy(ucinnostKotleString = it.replace(',', '.')))
                                    },
                                    modifier = Modifier
                                        .weight(1F)
                                        .padding(horizontal = 4.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    trailingIcon = {
                                        Text("%")
                                    }
                                )
                            }
                            Text(
                                """
                                Účinnost kotle volte:
                                60 % — vytápění pouze uhlím
                                65 % — kombinaci uhlí + dřevo
                                70 % — vytápění pouze dřevem
                                80 % — automatické kotle.
                            """.trimIndent(),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        Karticka {
                            Text("Stávající režim vytápění:", fontSize = 20.sp, modifier = Modifier.padding(bottom = 8.dp))
                            TypVypoctu.Tuhy.RezimVytapeni.values().forEach {
                                Card(
                                    onClick = {
                                        state = state.copy(typVypoctu = vypocet.copy(rezimVytapeni = it))
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        RadioButton(
                                            selected = vypocet.rezimVytapeni == it,
                                            onClick = {
                                                state = state.copy(typVypoctu = vypocet.copy(rezimVytapeni = it))
                                            },
                                        )
                                        Text(it.nazev)
                                    }
                                }
                            }
                        }

                        if (vypocet.potrebaTepla != 0.kWh)
                            Text("Potřeba tepla z paliva: ${vypocet.potrebaTepla} kWh", fontSize = 20.sp)


                        if (vypocet.potrebaTepla != 0.kWh) Karticka {
                            Text("Typ aplikace TČ:", fontSize = 20.sp)
                            TypVypoctu.Tuhy.TypAplikaceTC.values().forEach {
                                Card(
                                    onClick = {
                                        state = state.copy(typVypoctu = vypocet.copy(aplikace = it))
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        RadioButton(
                                            selected = vypocet.aplikace == it,
                                            onClick = {
                                                state = state.copy(typVypoctu = vypocet.copy(aplikace = it))
                                            },
                                        )
                                        Text(it.nazev)
                                    }
                                }
                            }
                        }
                    }

                    is TypVypoctu.Elektrina -> {
                        val vypocet = state.typVypoctu as TypVypoctu.Elektrina
                        Karticka {
                            Text("Zadejte spotřebu energie v nízkém tarifu:", fontSize = 20.sp, modifier = Modifier.padding(bottom = 8.dp))
                            OutlinedTextField(
                                value = vypocet.spotrebaNTstring,
                                onValueChange = {
                                    state = state.copy(typVypoctu = vypocet.copy(spotrebaNTstring = it))
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                trailingIcon = {
                                    Text("MWh")
                                }
                            )
                            Text("Zadejte spotřebu energie v vysokém tarifu:", fontSize = 20.sp, modifier = Modifier.padding(bottom = 8.dp))
                            OutlinedTextField(
                                value = vypocet.spotrebaVTstring,
                                onValueChange = {
                                    state = state.copy(typVypoctu = vypocet.copy(spotrebaVTstring = it))
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                trailingIcon = {
                                    Text("MWh")
                                }
                            )
                        }
                        /*Karticka {
                        Text("Typ sazby:", fontSize = 20.sp, modifier = Modifier.padding(bottom = 8.dp))
                        TypVypoctu.Elektrina.TypSazby.values().forEach {
                            Card(
                                onClick = {
                                    state = state.copy(typVypoctu = vypocet.copy(typSazby = it))
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    RadioButton(
                                        selected = vypocet.typSazby == it,
                                        onClick = {
                                            state = state.copy(typVypoctu = vypocet.copy(typSazby = it))
                                        },
                                    )
                                    Text(it.nazev)
                                }
                            }
                        }
                    }*/
                    }

                    is TypVypoctu.LTO -> {
                        val vypocet = state.typVypoctu as TypVypoctu.LTO
                        Karticka {
                            Text("Zadejte spotřebu topného oleje:", fontSize = 20.sp, modifier = Modifier.padding(bottom = 8.dp))
                            OutlinedTextField(
                                value = vypocet.spotrebaString,
                                onValueChange = {
                                    state = state.copy(typVypoctu = vypocet.copy(spotrebaString = it.replace(',', '.')))
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                trailingIcon = {
                                    Text("l")
                                }
                            )
                        }
                        Karticka {
                            Text("Účinnost kotle:", fontSize = 20.sp, modifier = Modifier.padding(bottom = 8.dp))
                            OutlinedTextField(
                                value = vypocet.ucinnostKotleString,
                                onValueChange = {
                                    state = state.copy(typVypoctu = vypocet.copy(ucinnostKotleString = it.replace(',', '.')))
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                trailingIcon = {
                                    Text("%")
                                }
                            )
                        }
                    }

                    is TypVypoctu.PENB -> {
                        val vypocet = state.typVypoctu as TypVypoctu.PENB
                        Karticka {
                            Text("Zadejte potřebu energie na vytápění:", fontSize = 20.sp, modifier = Modifier.padding(bottom = 8.dp))
                            OutlinedTextField(
                                value = vypocet.potrebaENaVytapeniString,
                                onValueChange = {
                                    state = state.copy(typVypoctu = vypocet.copy(potrebaENaVytapeniString = it))
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                trailingIcon = {
                                    Text("kWh")
                                }
                            )
                            Text("Zadejte potřebu energie na přípravu TV:", fontSize = 20.sp, modifier = Modifier.padding(bottom = 8.dp))
                            OutlinedTextField(
                                value = vypocet.potrebaENaTVString,
                                onValueChange = {
                                    state = state.copy(typVypoctu = vypocet.copy(potrebaENaTVString = it))
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                trailingIcon = {
                                    Text("kWh")
                                }
                            )
                        }
                    }

                    is TypVypoctu.Plyn -> {
                        val vypocet = state.typVypoctu as TypVypoctu.Plyn
                        Karticka {
                            Text("Zadejte spotřebu zemního plynu:", fontSize = 20.sp, modifier = Modifier.padding(bottom = 8.dp))
                            OutlinedTextField(
                                value = vypocet.spotrebaString,
                                onValueChange = {
                                    state = state.copy(typVypoctu = vypocet.copy(spotrebaString = it.replace(',', '.')))
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                trailingIcon = {
                                    Text("MWh")
                                }
                            )
                        }
                        Karticka {
                            Text("Účinnost kotle:", fontSize = 20.sp, modifier = Modifier.padding(bottom = 8.dp))
                            OutlinedTextField(
                                value = vypocet.ucinnostKotleString,
                                onValueChange = {
                                    state = state.copy(typVypoctu = vypocet.copy(ucinnostKotleString = it.replace(',', '.')))
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                trailingIcon = {
                                    Text("%")
                                }
                            )
                            Text(
                                """
                                Účinnost kotle volte:
                                70 % — atmosférický
                                80 % — turbo
                                90 % — kondenzační
                            """.trimIndent(),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        Karticka {
                            Card(
                                onClick = {
                                    state = state.copy(typVypoctu = vypocet.copy(varimePlynem = !vypocet.varimePlynem))
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Checkbox(
                                        checked = vypocet.varimePlynem,
                                        onCheckedChange = {
                                            state = state.copy(typVypoctu = vypocet.copy(varimePlynem = it))
                                        }
                                    )
                                    Text("Je plyn používán i k vaření?")
                                }
                            }
                        }
                    }

                    is TypVypoctu.TZ -> {
                        val vypocet = state.typVypoctu as TypVypoctu.TZ
                        Karticka {
                            Text("Zadejte tepelnou ztrátu objektu:", fontSize = 20.sp, modifier = Modifier.padding(bottom = 8.dp))
                            OutlinedTextField(
                                value = vypocet.tepelnaZtrataString,
                                onValueChange = {
                                    state = state.copy(typVypoctu = vypocet.copy(tepelnaZtrataString = it.replace(',', '.')))
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                trailingIcon = {
                                    Text("kW")
                                }
                            )
                        }
                    }

                    TypVypoctu.Nevybrano -> {}
                }

                if (state.typVypoctu.potrebaEnergie != 0.kWh || state.typVypoctu.tepelnaZtrata != 0.kW) {
                    when (state.typVypoctu.typ) {
                        TypVypoctu.TypTypuVypoctu.PotrebnaE -> Text("Potřeba energie od TČ: ${state.typVypoctu.potrebaEnergie} kWh", fontSize = 20.sp)
                        TypVypoctu.TypTypuVypoctu.ZtracenyQ -> Text("Tepelná ztráta objektu: ${state.typVypoctu.tepelnaZtrata} kW", fontSize = 20.sp)
                    }

                    if (!state.mamCerpadla) {
                        CircularProgressIndicator()
                    } else {

                        Karticka {
                            Text("Vyberte druh TČ:", fontSize = 20.sp)
                            state.druhyTC.forEach {
                                Card(
                                    onClick = {
                                        state = state.copy(druhTC = it, typTC = TepelnaCerpadla.DruhTC.TypTC(nazev = "", cerpadla = emptyList()))
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        RadioButton(
                                            selected = state.druhTC == it,
                                            onClick = {
                                                state = state.copy(druhTC = it, typTC = TepelnaCerpadla.DruhTC.TypTC(nazev = "", cerpadla = emptyList()))
                                            },
                                        )
                                        Text(it.nazev)
                                    }
                                }
                            }
                        }

                        if (state.typyTC.isNotEmpty()) {
                            Karticka {
                                Text("Vyberte typ TČ:", fontSize = 20.sp)
                                state.typyTC.forEach {

                                    Card(
                                        onClick = {
                                            state = state.copy(typTC = it)
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            RadioButton(
                                                selected = state.typTC == it,
                                                onClick = {
                                                    state = state.copy(typTC = it)
                                                },
                                            )
                                            Text(it.nazev)
                                        }
                                    }
                                }
                            }

                            if (state.doporuceneTC.isNotBlank()) {
                                Karticka {
                                    Text("Doporučené TČ: ${state.doporuceneTC}", fontSize = 20.sp)
                                }
                                Spacer(Modifier.height(16.dp))
                            }
                        }
                    }
                }

                var vice by remember { mutableStateOf(false) }

                Text(buildAnnotatedString {
                    if (vice) {
                        append("Detail návrhu:")
                        append(AnnotatedString(" (Schovat)", spanStyle = SpanStyle(color = MaterialTheme.colorScheme.tertiary)))
                    } else {
                        append(AnnotatedString("Zobrazit detail návrhu", spanStyle = SpanStyle(color = MaterialTheme.colorScheme.tertiary)))
                    }
                },
                    Modifier
                        .clickable {
                            vice = !vice
                        }
                        .padding(all = 8.dp))

                if (vice) Text(
                    "Ve všech případech uvažujeme i s připravou TV pro 4 osoby se spotřebou 40 l/os/den. Vstupní údaje pro návrh tepelného " +
                            "čerpadla musi být podloženy výpočtem. Potřeby energie na vytápěni a přípravu TV lze najit v Průkazu energetické náročnosti " +
                            "budovy. popt. stanovit podle ČSN EN ISO 52 016-1. Tepelná ztráta bývá uvedená v technické zprávě projektu vytápěni, nebo ji lze " +
                            "stanovit dle ČSN EN 12 831-1. \n" +
                            "Pokud je v objektu dalši významný spotřebič ohřívaný tepelným čerpadlem (bazén. vzduchotechnika, ...). který neni zahrnut ve výše " +
                            "uvedených výpočtech, kontaktujte nás na emailu poptavky@regulus.cz.",
                    Modifier.padding(all = 16.dp), fontSize = 14.sp, style = TextStyle(textAlign = TextAlign.Justify)
                )
            }
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Image(painterResource(R.drawable.logo), "Logo Regulus", modifier = Modifier.height(64.dp), contentScale = ContentScale.Fit)
            }
        }
    }
}

fun Double.nulaToString() = if (this == 0.0) "" else this.toString()
fun Int.nulaToString() = if (this == 0) "" else this.toString()

fun Boolean.toInt() = if (this) 1 else 0

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    NavrhTcTheme {
        ScreenContent()
    }
}

@Composable
fun Karticka(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier
            .padding(top = 8.dp, start = 8.dp, end = 8.dp)
            .fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 4.dp, horizontal = 8.dp)
        ) {
            content()
        }
    }

}

@Composable
fun <T> Dropdown(
    values: Iterable<T>,
    display: (T) -> String,
    currentValue: T,
    onValueClicked: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .wrapContentSize(Alignment.TopStart)
    ) {
        TextButton(
            onClick = { expanded = true }
        ) {
            Text(display(currentValue))
            Icon(
                Icons.Default.ArrowDropDown,
                contentDescription = "Rozbalit",
                modifier = Modifier.padding(start = 16.dp)
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            values.forEach {
                DropdownMenuItem(
                    text = { Text(display(it)) },
                    onClick = {
                        expanded = false
                        onValueClicked(it)
                    },
                )
            }
        }
    }
}


fun isOnline(ctx: Context): Boolean {
    val connectivityManager = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork) ?: return false

        return capabilities.hasTransport(
            NetworkCapabilities.TRANSPORT_CELLULAR
        ) || capabilities.hasTransport(
            NetworkCapabilities.TRANSPORT_WIFI
        ) || capabilities.hasTransport(
            NetworkCapabilities.TRANSPORT_ETHERNET
        )
    } else {
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnected
    }
}