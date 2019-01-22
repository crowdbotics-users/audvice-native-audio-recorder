/**
 * Sample React Native App
 * https://github.com/facebook/react-native
 *
 * @format
 * @flow
 */

import React, {Component} from 'react';
import {Platform, StyleSheet, Text, View, TouchableOpacity, Alert} from 'react-native';
import Permissions from 'react-native-permissions'

import AudioRecorder from './library_module'
// import AudioRecorder from 'react-native-audio-recorder'

type Props = {};
export default class App extends Component<Props> {

  constructor(props) {
    super(props)
    this.state = {
      initialized: false,
      hasPermissions: false
    }
  }

  componentDidMount() {
    this.permissionCheck()
  }

  permissionCheck() {
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

    this.audioRecoder.initialize('/sdcard/Android/media/com.google.android.talk/Ringtones/hangouts_incoming_call.ogg', -1)
    this.setState({
      initialized: true
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

  render() {
    return (
      <View style={styles.container}>
        <AudioRecorder
          style={{width: '100%', height: '25%', backgroundColor: 'red'}}
          height={100}
          width={100}
          ref={ref => this.audioRecoder = ref}
        />
        <View style={styles.buttonContainer}>
          <TouchableOpacity style={styles.button} onPress={this.onPressInit.bind(this)}>
            <Text style={{color: 'white'}}>init</Text>
          </TouchableOpacity>
          <TouchableOpacity style={styles.button} onPress={this.onPressinitWithFile.bind(this)}>
            <Text style={{color: 'white'}}>initWithFile</Text>
          </TouchableOpacity>
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
    justifyContent: 'space-between'
  },
  button: {
    height: 60,
    width: '18%',
    backgroundColor: 'blue',
    alignItems: 'center',
    justifyContent: 'center'
  },
});
