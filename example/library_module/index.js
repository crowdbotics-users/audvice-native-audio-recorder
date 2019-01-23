
import React from 'react'
import {
  NativeModules,
  Platform,
  Text,
  StyleSheet,
  ViewPropTypes,
  requireNativeComponent,
  UIManager,
  findNodeHandle
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

  initialize(filename, startTimeInMS) {
    RNAudioRecorder.initialize(findNodeHandle(this.recorderView), filename, startTimeInMS)
  }

  renderByFile(filename) {
    return RNAudioRecorder.renderByFile(findNodeHandle(this.recorderView), filename)
  }

  startRecording() {
    RNAudioRecorder.startRecording(findNodeHandle(this.recorderView))
  }

  stopRecording(){
    return RNAudioRecorder.stopRecording(findNodeHandle(this.recorderView))      
  }

  play() {
    RNAudioRecorder.play(findNodeHandle(this.recorderView))
  }

  cut(filename, fromTime, toTime){
    return RNAudioRecorder.cut(findNodeHandle(this.recorderView), filename, fromTime, toTime)
  }

  destroy() {
    return RNAudioRecorder.destroy(findNodeHandle(this.recorderView))
  }

  render() {
    const {
      width,
      height,
      onScroll,
      pixelsPerSecond,
      plotLineColor      
    } = this.props
    return(
      <RNAudioRecorderView style={this.props.style} status={this.state.lastaction}
        ref={ref => this.recorderView = ref}
        onScroll={onScroll}
        pixelsPerSecond={pixelsPerSecond}
        plotLineColor={plotLineColor}/>
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
