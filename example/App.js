/**
 * Sample React Native App
 * https://github.com/facebook/react-native
 *
 * @format
 * @flow
 */

import React, {Component} from 'react';
import {Platform, StyleSheet, Text, View, TouchableOpacity} from 'react-native';

import AudioRecorder from './library_module'
// import AudioRecorder from 'react-native-audio-recorder'

type Props = {};
export default class App extends Component<Props> {

  constructor(props) {
    super(props)
  }

  componentDidMount() {
    this.audioRecoder.initialize()
  }

  onPressPlay() {
    this.audioRecoder.play()
  }

  onPressStop() {
    this.audioRecoder.stopRecording()
  }

  onPressStart() {
    this.audioRecoder.startRecording('', -1)
  }

  render() {
    return (
      <View style={styles.container}>
        <AudioRecorder 
          height={100}
          width={100}
          ref={ref => this.audioRecoder = ref}
        />
        <View style={styles.buttonContainer}>
          <TouchableOpacity style={styles.button} onPress={this.onPressStart.bind(this)}>
            <Text style={{color: 'white'}}>start</Text>
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
    flexDirection: 'row'
  },
  button: {
    width: 60,
    height: 60,
    borderRadius: 30,
    margin: 20,    
    backgroundColor: 'blue',
    alignItems: 'center',
    justifyContent: 'center'
  },
});
