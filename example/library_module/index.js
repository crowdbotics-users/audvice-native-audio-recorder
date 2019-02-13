
import React from 'react'
import {
  NativeModules,
  Platform,
  Text,
  StyleSheet,
  ViewPropTypes,
  requireNativeComponent,
  DeviceEventEmitter,
  findNodeHandle
} from 'react-native'
import PropTypes from 'prop-types'

const RNAudioRecorder = NativeModules.RNAudioRecorder || NativeModules.RNAudioRecorderViewManager
const RNAudioRecorderView = requireNativeComponent('RNAudioRecorderView')

export default class AudioRecorder extends React.Component {
  constructor(props) {
    super(props)
    DeviceEventEmitter.addListener('onPlayFinished', this._onPlayFinished.bind(this))
  }

  _onPlayFinished() {
    if (this.props.onPlayFinished)
    {
      this.props.onPlayFinished()
    }
  }

  initialize(filename, startTimeInMS) {
    return RNAudioRecorder.initialize(findNodeHandle(this.recorderView), filename, startTimeInMS)
  }

  renderByFile(filename) {
    return RNAudioRecorder.renderByFile(findNodeHandle(this.recorderView), filename)
  }

  startRecording() {
    return RNAudioRecorder.startRecording(findNodeHandle(this.recorderView))
  }

  stopRecording(){
    return RNAudioRecorder.stopRecording(findNodeHandle(this.recorderView))      
  }

  play() {
    return RNAudioRecorder.play(findNodeHandle(this.recorderView))
  }

  pause() {
    return RNAudioRecorder.pause(findNodeHandle(this.recorderView))
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
      plotLineColor,
      timeTextColor,
      timeTextSize
    } = this.props
    return(
      <RNAudioRecorderView style={this.props.style}
        ref={ref => this.recorderView = ref}
        onScroll={onScroll}
        pixelsPerSecond={pixelsPerSecond}
        plotLineColor={plotLineColor}
        timeTextColor={timeTextColor}
        timeTextSize={timeTextSize} />
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
  timeTextColor: PropTypes.string,
  timeTextSize: PropTypes.number,
  onPlayFinished: PropTypes.func
}

AudioRecorder.defaultProps = {
  onScroll: true,
  height: 0,
  width: 0,
  pixelsPerSecond: 50,
  plotLineColor: 'white',
  timeTextColor: 'white',
  timeTextSize: 20
}
