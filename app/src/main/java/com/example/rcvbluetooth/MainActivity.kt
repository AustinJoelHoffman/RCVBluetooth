package com.example.rcvbluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowLeft
import androidx.compose.material.icons.automirrored.rounded.ArrowRight
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.ArrowDropUp
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.KeyboardDoubleArrowLeft
import androidx.compose.material.icons.rounded.KeyboardDoubleArrowRight
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rcvbluetooth.domain.OpCode
import com.example.rcvbluetooth.domain.BluetoothClassicManager
import com.example.rcvbluetooth.domain.ConnectionState
import com.example.rcvbluetooth.domain.ControlViewModel
import com.example.rcvbluetooth.domain.ControlViewModelFactory
import com.example.rcvbluetooth.ui.theme.RCVBluetoothTheme

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        requestPermissions()

        enableEdgeToEdge()
        setContent {
            RCVBluetoothTheme {
                // Initialize Bluetooth
                val classicManager = remember { BluetoothClassicManager(this) }
                val vm: ControlViewModel = viewModel(
                    factory = ControlViewModelFactory(classicManager)
                )
                
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Surface(
                        modifier = Modifier.padding(innerPadding),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        MainControlScreen(vm)
                    }
                }
            }
        }
    }

    private fun requestPermissions() {
        val permissions =
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        permissionLauncher.launch(permissions)
    }
}

@Composable
fun MainControlScreen(vm: ControlViewModel) {
    val connectionState by vm.connectionState.collectAsState()
    val pairedDevices = vm.getPairedDevices()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Bluetooth Card
        BluetoothConnectionCard(
            connectionState = connectionState,
            pairedDevices = pairedDevices,
            onConnect = { vm.connect(it) },
            onDisconnect = { vm.disconnect() }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Steering Controls
        SteeringControls(
            onCommand = { vm.sendCommand(it) },
            onSpeedChange = { vm.sendSpeed(it) }
        )
    }
}

@Composable
fun BluetoothConnectionCard(
    connectionState: ConnectionState,
    pairedDevices: List<BluetoothDevice>,
    onConnect: (BluetoothDevice) -> Unit,
    onDisconnect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Bluetooth,
                    contentDescription = null,
                    tint = Color(0xFF0082FC),
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Bluetooth: $connectionState", style = MaterialTheme.typography.titleMedium)
            }

            if (connectionState == ConnectionState.Connected) {
                Button(onClick = onDisconnect, modifier = Modifier.padding(top = 8.dp)) {
                    Text("Disconnect")
                }
            } else {
                Text(text = "Paired Devices:", modifier = Modifier.padding(vertical = 8.dp))
                LazyColumn(modifier = Modifier.height(120.dp)) {
                    items(pairedDevices) { device ->
                        @SuppressLint("MissingPermission")
                        @Suppress("DEPRECATION")
                        Text(
                            text = device.name ?: "Unknown Device",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onConnect(device) }
                                .padding(vertical = 4.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedControlButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = ButtonDefaults.shape,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "scale"
    )
    
    Button(
        onClick = onClick,
        modifier = modifier.graphicsLayer(scaleX = scale, scaleY = scale),
        interactionSource = interactionSource,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isPressed) containerColor.copy(alpha = 0.7f) else containerColor
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 6.dp,
            pressedElevation = 2.dp
        ),
        contentPadding = PaddingValues(0.dp),
        content = { content() }
    )
}

@Composable
fun HoldToRunButton(
    onPress: () -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "scale"
    )

    var hasBeenPressed by remember { mutableStateOf(false) }
    LaunchedEffect(isPressed) {
        if (isPressed) {
            onPress()
            hasBeenPressed = true
        } else if (hasBeenPressed) {
            onRelease()
        }
    }
    
    Button(
        onClick = {  }, // See firmware
        modifier = modifier.graphicsLayer(scaleX = scale, scaleY = scale),
        interactionSource = interactionSource,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isPressed) containerColor.copy(alpha = 0.7f) else containerColor
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 6.dp,
            pressedElevation = 2.dp
        ),
        contentPadding = PaddingValues(0.dp),
        content = {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                content()
            }
        }
    )
}

@Composable
fun SteeringControls(
    onCommand: (OpCode) -> Unit,
    onSpeedChange: (Int) -> Unit
) {
    var maxSpeed by remember { mutableFloatStateOf(180f) }
    var movingCount by remember { mutableIntStateOf(0) }
    val isMoving = movingCount > 0
    var isDraggingSlider by remember { mutableStateOf(false) }

    val currentSpeed by animateFloatAsState(
        targetValue = if (isMoving) maxSpeed else 0f,
        animationSpec = tween(durationMillis = 500),
        label = "speedRamp"
    )

    LaunchedEffect(currentSpeed) {
        onSpeedChange(currentSpeed.toInt())
        if (currentSpeed == 0f && !isMoving) {
            onCommand(OpCode.STOP)
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Spin Left and Forward
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            HoldToRunButton(
                onPress = { 
                    movingCount++
                    onCommand(OpCode.SPIN_LEFT) 
                },
                onRelease = { 
                    movingCount--
                },
                modifier = Modifier.size(80.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardDoubleArrowLeft,
                    contentDescription = "MODE_SPIN_LEFT",
                    modifier = Modifier.size(48.dp),
                    tint = Color.White
                )
            }
            HoldToRunButton(
                onPress = { 
                    movingCount++
                    onCommand(OpCode.FORWARD) 
                },
                onRelease = { 
                    movingCount--
                },
                modifier = Modifier.size(80.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.ArrowDropUp,
                    contentDescription = "MODE_FWD",
                    modifier = Modifier.size(64.dp),
                    tint = Color.White
                )
            }
            // Spin Rigth and Forward
            HoldToRunButton(
                onPress = { 
                    movingCount++
                    onCommand(OpCode.SPIN_RIGHT) 
                },
                onRelease = { 
                    movingCount--
                },
                modifier = Modifier.size(80.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardDoubleArrowRight,
                    contentDescription = "MODE_SPIN_RIGHT",
                    modifier = Modifier.size(48.dp),
                    tint = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Left
            HoldToRunButton(
                onPress = { 
                    movingCount++
                    onCommand(OpCode.LEFT) 
                },
                onRelease = { 
                    movingCount--
                },
                modifier = Modifier.size(80.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowLeft,
                    contentDescription = "MODE_PIVOT_LEFT",
                    modifier = Modifier.size(64.dp),
                    tint = Color.White
                )
            }
            
            // Stop
            AnimatedControlButton(
                onClick = { 
                    movingCount = 0
                    onCommand(OpCode.STOP) 
                },
                modifier = Modifier.size(80.dp), 
                containerColor = Color.Red,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("STOP")
            }

            // Right
            HoldToRunButton(
                onPress = { 
                    movingCount++
                    onCommand(OpCode.RIGHT) 
                },
                onRelease = { 
                    movingCount--
                },
                modifier = Modifier.size(80.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowRight,
                    contentDescription = "MODE_PIVOT_RIGHT",
                    modifier = Modifier.size(64.dp),
                    tint = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Back
        HoldToRunButton(
            onPress = { 
                movingCount++
                onCommand(OpCode.BACKWARD) 
            },
            onRelease = { 
                movingCount--
            },
            modifier = Modifier.size(80.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.ArrowDropDown,
                contentDescription = "MODE_BWD",
                modifier = Modifier.size(64.dp),
                tint = Color.White
            )
        }

        Spacer(modifier = Modifier.height(48.dp))
        
        Text(text = "Speed: ${currentSpeed.toInt()}", style = MaterialTheme.typography.labelLarge)
        Slider(
            value = if (isDraggingSlider) maxSpeed else if (isMoving || currentSpeed > 0) currentSpeed else maxSpeed,
            onValueChange = { 
                maxSpeed = it
                isDraggingSlider = true
            },
            onValueChangeFinished = { 
                isDraggingSlider = false
            },
            valueRange = 0f..255f,
            modifier = Modifier.padding(horizontal = 48.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Max Speed: ${maxSpeed.toInt()}", style = MaterialTheme.typography.bodyMedium)
    }
}
