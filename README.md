
# react-native-audio-recorder for Audvice

## Getting started

`$ npm install --save https://github.com/crowdbotics-users/audvice-native-audio-recorder.git`

### Mostly automatic installation

`$ react-native link react-native-audio-recorder`

### Manual installation


#### iOS

1. In XCode, in the project navigator, right click `Libraries` ➜ `Add Files to [your project's name]`
2. Go to `node_modules` ➜ `react-native-audio-recorder` and add `RNAudioRecorder.xcodeproj`
3. In XCode, in the project navigator, select your project. Add `libRNAudioRecorder.a` to your project's `Build Phases` ➜ `Link Binary With Libraries`
4. Run your project (`Cmd+R`)<

#### Android

1. Open up `android/app/src/main/java/[...]/MainActivity.java`
  - Add `import com.reactlibrary.RNAudioRecorderPackage;` to the imports at the top of the file
  - Add `new RNAudioRecorderPackage()` to the list returned by the `getPackages()` method
2. Append the following lines to `android/settings.gradle`:
  	```
  	include ':react-native-audio-recorder'
  	project(':react-native-audio-recorder').projectDir = new File(rootProject.projectDir, 	'../node_modules/react-native-audio-recorder/android')
  	```
3. Insert the following lines inside the dependencies block in `android/app/build.gradle`:
  	```
      compile project(':react-native-audio-recorder')
  	```

### Post installation
On *iOS* you need to add a usage description to `Info.plist`:

```xml
<key>NSMicrophoneUsageDescription</key>
<string>This sample uses the microphone to record your speech and convert it to text.</string>
```

Add *AudioToolbox.framework* in project setting

On *Android* you need to add a permission to `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
```

### Permission Check
Also, android above `Marshmallow` needs runtime permission to record audio. Using [react-native-permissions](https://github.com/yonahforst/react-native-permissions) will help you out with this problem. Below is sample usage before when started the recording.

In iOS, please make sure the project link with react-native-permissions and add following dict in plist file.

```
  <key>NSAppleMusicUsageDescription</key>
	<string>$(PRODUCT_NAME) allows to access Media Library</string>
  <key>NSMicrophoneUsageDescription</key>
	<string>$(PRODUCT_NAME) uses the microphone to record your voice.</string>
```

```javascript
  permissionCheck() {
    if (Platform.OS === 'android') {
      Permissions.checkMultiple(['microphone', 'storage'])
      .then(response => {        
        if (response.microphone !== 'authorized') {
          Permissions.request('microphone')
          .then(response => {
            if (response.storage !== 'authorized') {
              Permissions.request('storage')
              .then(response => {
                
              })
            }else{              
            }
          })
        } else {   
          if (response.storage !== 'authorized') {
            Permissions.request('storage')
            .then(response => {
            })
          }else{            
            this.setState({
              hasPermissions: true
            })
          }       
        }       
      })
    } else {
      Permissions.checkMultiple(['microphone', 'mediaLibrary'])
      .then(response => {  
        
        console.warn(response)      
        if (response.microphone !== 'authorized') {
          Permissions.request('microphone')
          .then(response => {
            if (response.mediaLibrary !== 'authorized') {
              Permissions.request('mediaLibrary')
              .then(response => {
              })
            }else{              
            }
          })
        } else {   
          if (response.mediaLibrary !== 'authorized') {
            Permissions.request('mediaLibrary')
            .then(response => {
              if (response == 'authorized') {
                this.setState({
                  hasPermissions: true
                })
              }
            })
          }else{
            this.setState({
              hasPermissions: true
            })
          }       
        }       
      })
    }
  }
```

## Usage
```javascript
import React, {Component} from 'react';
import {Platform, StyleSheet, Text, View, TouchableOpacity, Alert} from 'react-native';
import Permissions from 'react-native-permissions'
import AudioRecorder from 'react-native-audio-recorder'

export default class App extends Component<Props> {

  constructor(props) {
    super(props)
    this.state = {
      initialized: false,
      hasPermissions: false,
      result: 'No Result'
    }
  }

  componentDidMount() {
    this.permissionCheck()
  }

  permissionCheck() {
    if (Platform.OS === 'android') {
      Permissions.checkMultiple(['microphone', 'storage'])
      .then(response => {        
        if (response.microphone !== 'authorized') {
          Permissions.request('microphone')
          .then(response => {
            if (response.storage !== 'authorized') {
              Permissions.request('storage')
              .then(response => {
                
              })
            }else{              
            }
          })
        } else {   
          if (response.storage !== 'authorized') {
            Permissions.request('storage')
            .then(response => {
            })
          }else{            
            this.setState({
              hasPermissions: true
            })
          }       
        }       
      })
    } else {
      Permissions.checkMultiple(['microphone', 'mediaLibrary'])
      .then(response => {  
        
        console.warn(response)      
        if (response.microphone !== 'authorized') {
          Permissions.request('microphone')
          .then(response => {
            if (response.mediaLibrary !== 'authorized') {
              Permissions.request('mediaLibrary')
              .then(response => {
              })
            }else{              
            }
          })
        } else {   
          if (response.mediaLibrary !== 'authorized') {
            Permissions.request('mediaLibrary')
            .then(response => {
              if (response == 'authorized') {
                this.setState({
                  hasPermissions: true
                })
              }
            })
          }else{
            this.setState({
              hasPermissions: true
            })
          }       
        }       
      })
    }
  }

  onPressPlay() {
    if (!this.state.initialized) {
      console.warn('Please call init method.')
      return
    }
    this.audioRecoder.play()
  }

  onPressStop() {
    if (!this.state.initialized) {
      console.warn('Please call init method.')
      return
    }
    this.audioRecoder.stopRecording()
      .then(res => {
        this.setState({
          result: `${res.filepath} : ${res.duration} ms`
        })
      })
      .catch((err) => {
        this.setState({
          result: `error: ${err}`
        })
      })
  }

  onPressStart() {
    if (!this.state.initialized) {
      console.warn('Please call init method.')
      return
    }
    this.audioRecoder.startRecording()
  }

  onPressinitWithFile() {
    if (!this.state.hasPermissions) {
      Alert.alert(
        'Permission Errors',
        'Please make sure permissions enabled, and try again',
        [
          {text: 'Try Again', onPress:this.permissionCheck.bind(this)}
        ]
      )
      return
    }

    this.audioRecoder.initialize('/sdcard/Android/media/com.google.android.talk/Ringtones/hangouts_incoming_call.ogg', 2000)
    this.setState({
      initialized: true
    })
  }

  onPressRenderByFile() {
    if (!this.state.hasPermissions) {
      Alert.alert(
        'Permission Errors',
        'Please make sure permissions enabled, and try again',
        [
          {text: 'Try Again', onPress:this.permissionCheck.bind(this)}
        ]
      )
      return
    }

    this.audioRecoder.renderByFile('/sdcard/Android/media/com.google.android.talk/Ringtones/hangouts_incoming_call.ogg')
    .then(res => {
      this.setState({
        result: res,
        initialized: true
      })
    })
    .catch((err) => {
      this.setState({
        result: `error: ${err}`
      })
    })
  }

  onPressInit() {
    if (!this.state.hasPermissions) {
      Alert.alert(
        'Permission Errors',
        'Please make sure permissions enabled, and try again',
        [
          {text: 'Try Again', onPress:this.permissionCheck.bind(this)}
        ]
      )
      return
    }
    this.audioRecoder.initialize('', -1)
    this.setState({
      initialized: true
    })
  }

  onPressCut() {
    if (!this.state.hasPermissions) {
      Alert.alert(
        'Permission Errors',
        'Please make sure permissions enabled, and try again',
        [
          {text: 'Try Again', onPress:this.permissionCheck.bind(this)}
        ]
      )
      return
    }
    this.audioRecoder.cut('/sdcard/Android/media/com.google.android.talk/Ringtones/hangouts_incoming_call.ogg', 500, 2000)
    .then(res => {
      this.setState({
        result: `${res.filepath} : ${res.duration} ms`,
        initialized: true
      })
    })
    .catch((err) => {
      this.setState({
        result: `error: ${err}`
      })
    })
  }

  render() {
    return (
      <View style={styles.container}>
        <AudioRecorder
          style={{width: '75%', height: 200, backgroundColor: 'green'}}
          plotLineColor={'yellow'}
          timeTextColor={'white'}
          timeTextSize={12}
          onScroll={true}
          pixelsPerSecond={200}
          ref={ref => this.audioRecoder = ref}
        />
        <View style={styles.buttonContainer}>
          <TouchableOpacity style={styles.button} onPress={this.onPressInit.bind(this)}>
            <Text style={{color: 'white'}}>init</Text>
          </TouchableOpacity>
          <TouchableOpacity style={styles.button} onPress={this.onPressinitWithFile.bind(this)}>
            <Text style={{color: 'white'}}>initWithFile</Text>
          </TouchableOpacity>
          <TouchableOpacity style={styles.button} onPress={this.onPressRenderByFile.bind(this)}>
            <Text style={{color: 'white'}}>renderByFile</Text>
          </TouchableOpacity>
          <TouchableOpacity style={styles.button} onPress={this.onPressCut.bind(this)}>
            <Text style={{color: 'white'}}>Cut</Text>
          </TouchableOpacity>
        </View>
        <View style={styles.buttonContainer}>
          <TouchableOpacity style={styles.button} onPress={this.onPressStart.bind(this)}>
            <Text style={{color: 'white'}}>start/pause</Text>
          </TouchableOpacity>
          <TouchableOpacity style={styles.button} onPress={this.onPressStop.bind(this)}>
            <Text style={{color: 'white'}}>stop</Text>
          </TouchableOpacity>
          <TouchableOpacity style={styles.button} onPress={this.onPressPlay.bind(this)}>
            <Text style={{color: 'white'}}>play</Text>
          </TouchableOpacity>
        </View>
        <Text>{this.state.result}</Text>
      </View>
    );
  }
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#F5FCFF',
  },
  buttonContainer: {
    flexDirection: 'row',
    width: '100%',
    justifyContent: 'space-around',
    marginVertical: 10
  },
  button: {
    height: 60,
    width: '25%',
    backgroundColor: 'blue',
    alignItems: 'center',
    justifyContent: 'center'
  },
});
```
  
