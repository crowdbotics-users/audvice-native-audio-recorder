
# react-native-audio-recorder for Audvice

## Getting started

`$ npm install react-native-audio-recorder --save`

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

On *Android* you need to add a permission to `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
```
Also, android above `Marshmallow` needs runtime permission to record audio. Using [react-native-permissions](https://github.com/yonahforst/react-native-permissions) will help you out with this problem. Below is sample usage before when started the recording.
```javascript
if (Platform.OS === 'android') {
  Permissions.checkMultiple(['microphone', 'storage'])
	.then(response => {        
		var permissionArray = []
		if (response.microphone !== 'authorized') {
			Permissions.request('microphone')
			.then(response => {
				if (response.storage !== 'authorized') {
					Permissions.request('storage')
					.then(response => {
						this.audioRecoder.initialize()
					})
				}else{              
					this.audioRecoder.initialize()
				}
			})
		} else {   
			if (response.storage !== 'authorized') {
				Permissions.request('storage')
				.then(response => {
					this.audioRecoder.initialize()
				})
			}else{            
				this.audioRecoder.initialize()
			}       
		}       
	})
}
```

## Usage
```javascript
import AudioRecorder from 'react-native-audio-recorder';

// TODO: What to do with the module?
AudioRecorder;
```
  
