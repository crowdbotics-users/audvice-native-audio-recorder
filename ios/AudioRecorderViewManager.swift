//
//  AudioRecorderViewManager.swift
//  reactnativeaudiorecorder
//

import Foundation
import UIKit

import AudioKit
import AudioKitUI

// Represents the AudioRecorderViewManager which manages our AudioRecorderView Module
@objc(AudioRecorderViewManager)
class AudioRecorderViewManager : RCTViewManager {

    // The native ui view
    private var currentView: AudioRecorderView?

    // The promise response
    private var jsonArray: JSON = [
        "success": false,
        "error": "",
        "value": ["fileName": "", "fileDurationInMs": "0"]
        ]

    // Error for testing error handling
    // Can be removed anytime later
    enum TestError: Error {
        case runtimeError(String)
    }

    // Instantiates the view
    override func view() -> AudioRecorderView {
        let newView = AudioRecorderView()
        self.currentView = newView
        return newView
    }

    // Tells React Native to use Main Thread
    override class func requiresMainQueueSetup() -> Bool {
        return true
    }

    // Received properties from React Native and sets them on the view
    @objc public func passProperties(_ backgroundColor:String, propLineColor lineColor:String) {
        DispatchQueue.main.async {
            self.currentView?.layoutSubviews()
        }
    }

    // Sets the dimensions of the AudioRecorderView to the component dimensions received from React Native
    @objc public func setDimensions(_ width:Double, dimHeight height:Double) {
        self.currentView?.componentWidth = width
        self.currentView?.componentHeight = height

        DispatchQueue.main.async {
            self.currentView?.layoutSubviews()
        }
    }

    // Cleans up
    private func cleanupRecorder(onSuccess: @escaping (Bool) -> Void, onError: @escaping (Error) -> Void) {
        do {
            // Completed without error
            onSuccess(true)
        } catch {
          // Failed cleanup is not necessarily an error, thus success is ok here
            self.jsonArray["error"].stringValue = error.localizedDescription + " - Cleanup not necessary."
            onSuccess(true)
        }
    }

    // Init new recording session
    private func initRecorderSession(onSuccess: @escaping (Bool) -> Void, onError: @escaping (Error) -> Void) {
        do {
          // Completed without error
            onSuccess(true)
        } catch {
            onError(error)
        }
    }

    // Setup virtual devices like, for example, mixer
    private func setupVirtualDevices(onSuccess: @escaping (Bool) -> Void, onError: @escaping (Error) -> Void) {
        do {
            // Completed without error
            onSuccess(true)
        } catch {
            // Aborted with error
            onError(error)
        }
    }

    // Start the AudioKit engine
    private func startEngine(onSuccess: @escaping (Bool) -> Void, onError: @escaping (Error) -> Void) {
        do {
            // Completed without error
            onSuccess(true)
        } catch {
            // Aborted with error
            onError(error)
        }
    }

    // Setup the final tape which will contain all audio data of one session
    private func setupFinalTape(onSuccess: @escaping (Bool) -> Void, onError: @escaping (Error) -> Void) {
        do {
            // Completed without error
            onSuccess(true)
        }
        catch {
            onError(error)
        }
    }

    // Exports the final tape to a file
    private func exportFinalTapeToFile(onSuccess: @escaping (Bool) -> Void, onError: @escaping (Error) -> Void) {
    // Store the audio data (final tape) permanently on the device's storage
        do{
            // Completed without error
            onSuccess(true)
        }
        catch{
            // Aborted with error
            self.jsonArray["error"].stringValue = (error.localizedDescription) + " - Export failed."
            onError(error)
        }
    }

    // Resets recorder and current tape
    private func resetDataFromPreviousRecording(onSuccess: @escaping (Bool) -> Void, onError: @escaping (Error) -> Void) {
        do {
            // Completed without error
            onSuccess(true)
        } catch {
            print("Reset data failed.")

            // Aborted with error
            self.jsonArray["error"].stringValue = error.localizedDescription
            onError(error)
        }
    }

    // Partially overwrites the previous tape with the content of the current tape
    private func overwritePartially(onSuccess: @escaping (Bool) -> Void, onError: @escaping (Error) -> Void) {
        do {
            // Completed without error
            onSuccess(true)
        } catch {
            print("Partial overwrite failed.")

            // Aborted with error
            self.jsonArray["error"].stringValue = error.localizedDescription
            onError(error)
        }
    }

    // Deletes the file so that it can be replaced without problems
    private func deleteFile(onSuccess: @escaping (Bool) -> Void, onError: @escaping (Error) -> Void) {
    // Delete the previous file
        do {
            // Completed without error
            onSuccess(true)
        } catch {
            // Success is okay here because we just need to delete the file if AudioKit didn't do it by itself
            // and if it already did an error will occur here that it cannot deleted anymore so it's ok to just move on
            onSuccess(true)
        }
    }

    // Creates one final tape out of others and stores it as an audio file on the device's storage
    private func createAndStoreTapeFromRecordings(onSuccess: @escaping (Bool) -> Void, onError: @escaping (Error) -> Void) {
        // Create a new file
        do {
            // Completed without error
            onSuccess(true)
        } catch {
            // Aborted with error
            self.jsonArray["error"].stringValue = error.localizedDescription
            onError(error)
        }
    }

    // Instantiates all the things needed for recording
    @objc public func setupRecorder(_ resolve:RCTPromiseResolveBlock, rejecter reject:@escaping RCTPromiseRejectBlock) {
        // Define the error storage
        var e : Error?;

        // Cleanup before initialize the new session
        cleanupRecorder(
            onSuccess: { success in
                self.jsonArray["success"] = true
            },
            onError: { error in
                self.jsonArray["success"] = false
                e = error
            }
        )

        // Init a new recording session
        initRecorderSession(
            onSuccess: { success in
                self.jsonArray["success"] = true
            },
            onError: { error in
                self.jsonArray["success"] = false
                e = error
            }
        )

        // Setup the virtual devices e. g. mixer
        setupVirtualDevices(
            onSuccess: { success in
                self.jsonArray["success"] = true
            },
            onError: { error in
                self.jsonArray["success"] = false
                e = error
            }
        )

        // Start the AudioKit engine
        startEngine(
            onSuccess: { success in
                self.jsonArray["success"] = true
            },
            onError: { error in
                self.jsonArray["success"] = false
                e = error
            }
        )

        // Setup the final tape
        setupFinalTape(
            onSuccess: { success in
                self.jsonArray["success"] = true
            },
            onError: { error in
                self.jsonArray["success"] = false
                e = error
            }
        )
    }

    // Starts the recording of audio
    @objc public func startRecording(_ fileName:NSString, startTime startTimeInMs:Double, resolver resolve:RCTPromiseResolveBlock, rejecter reject:RCTPromiseRejectBlock) {
        do {
          // Inform bridge/React about success
          self.jsonArray["success"] = true
          resolve(self.jsonArray.rawString());
        } catch {
          print("Recording failed.")

          // Inform bridge/React about error
          self.jsonArray["error"].stringValue = error.localizedDescription
          self.jsonArray["success"] = false
          reject("Error", self.jsonArray.rawString(), error)
        }
    }

    // Stops audio recording and stores the recorded data in a file
    @objc public func stopRecording(_ resolve:@escaping RCTPromiseResolveBlock, rejecter reject:@escaping RCTPromiseRejectBlock) {
        // Define the error storage
        var e : Error?;
        // Create tape from and store the recorded audio data
        createAndStoreTapeFromRecordings(
          onSuccess: { success in
            self.jsonArray["success"] = true
          },
          onError: { error in
            self.jsonArray["success"] = false
            e = error
            reject("Error", self.jsonArray.rawString(), e)
          }
        )

        // Stop AudioKit
        do {
          resolve(self.jsonArray.rawString());
        } catch {
          self.jsonArray["error"].stringValue = error.localizedDescription + " - Cleanup not necessary."
          reject("Error", self.jsonArray.rawString(), e)
        }
    }
}
