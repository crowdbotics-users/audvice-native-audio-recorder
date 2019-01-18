
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

  initialize() {
    RNAudioRecorder.initialize(findNodeHandle(this.recorderView))
  }

  startRecording(filename, startTimeInMS) {
    RNAudioRecorder.startRecording(findNodeHandle(this.recorderView), filename, startTimeInMS)
  }

  stopRecording(){
    RNAudioRecorder.stopRecording(findNodeHandle(this.recorderView))
      .then(res => {
        console.warn(res);
      })
      .catch((err) => console.warn(err))
  }

  play() {
    RNAudioRecorder.play(findNodeHandle(this.recorderView))
  }

  renderByFile(filename){
    RNAudioRecorder.renderByFile(findNodeHandle(this.recorderView), filename)
  }

  cut(filename, fromTime, toTime){
    RNAudioRecorder.cut(findNodeHandle(this.recorderView), filename, fromTime, toTime)
  }

  destroy() {
    RNAudioRecorder.destroy(findNodeHandle(this.recorderView))
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
