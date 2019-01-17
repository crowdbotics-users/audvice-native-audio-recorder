
import React from 'react'
import {
  NativeModules,
  View,
  Text,
  StyleSheet,
  ViewPropTypes,
  requireNativeComponent
} from 'react-native'
import PropTypes from 'prop-types'

const { RNAudioRecorder } = NativeModules
const RNAudioRecorderView = requireNativeComponent('RNAudioRecorderView')

export default class AudioRecorder extends React.Component {
  constructor(props) {
    super(props)
    this.state = {
      lastaction: ''
    }
  }

  initialize() {
    this.setState({
      lastaction: 'initialize'
    })
  }

  startRecording(filename, startTimeInMS) {
    this.setState({
      lastaction: `startRecording filename: ${filename}, starttime: ${startTimeInMS}`
    })
  }

  stopRecording(){
    this.setState({
      lastaction: `stopRecording`
    })
  }

  play() {
    this.setState({
      lastaction: `play`
    })
  }

  renderByFile(filename){
    this.setState({
      lastaction: `renderByFile filename: ${filename}`
    })
  }

  cut(filename, fromTime, toTime){
    this.setState({
      lastaction: `cut filename: ${filename}, fromTime: ${fromTime}, toTime: ${toTime}`
    })
  }

  destroy() {
    this.setState({
      lastaction: `destroy`
    })
  }

  render() {
    return(
      <View style={[this.props.style, {backgroundColor: 'grey', }]}>
        <Text>JS Demo Module</Text>
        <Text>{this.state.lastaction}</Text>
        <RNAudioRecorderView style={{width: 100, height: 20}}/>
      </View>
    )
  }
}

AudioRecorder.propTypes = {
  ...ViewPropTypes,
  onScroll: PropTypes.bool,
  height: PropTypes.number,
  width: PropTypes.number,
  pixelsPerSecond: PropTypes.number,
  plotLineColor: PropTypes.string,
  timeLineStyle: Text.propTypes.style
}

AudioRecorder.defaultProps = {
  onScroll: true,
  height: 0,
  width: 0,
  pixelsPerSecond: 50,
  plotLineColor: 'white',
  timeLineStyle: {
    color: 'white'
  }
}
