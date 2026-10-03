// Re-encodes a phone video into a small, streamable H.264/AAC MP4 for the in-app promo popup.
//
//   swiftc -O scripts/promo-video/transcode.swift -o /tmp/promo-transcode
//   /tmp/promo-transcode in.mp4 out.mp4 [width=720] [height=1280] [videoKbps=1300] [fps=30]
//
// Why not ffmpeg: it isn't installed on this Mac, and AVFoundation ships with macOS. Output is
// H.264 Main profile (plays everywhere, unlike HEVC) with the moov atom at the front so the
// player can start before the whole file has downloaded.
import AVFoundation
import Foundation

let args = CommandLine.arguments
guard args.count >= 3 else {
    print("usage: transcode in.mp4 out.mp4 [width] [height] [videoKbps] [fps]")
    exit(2)
}
let inURL = URL(fileURLWithPath: args[1])
let outURL = URL(fileURLWithPath: args[2])
let outW = args.count > 3 ? Int(args[3])! : 720
let outH = args.count > 4 ? Int(args[4])! : 1280
let kbps = args.count > 5 ? Int(args[5])! : 1300
let fps = args.count > 6 ? Int32(args[6])! : 30

try? FileManager.default.removeItem(at: outURL)
let asset = AVURLAsset(url: inURL)

func run() async throws {
    guard let vTrack = try await asset.loadTracks(withMediaType: .video).first else { fatalError("no video track") }
    let natural = try await vTrack.load(.naturalSize)
    let pref = try await vTrack.load(.preferredTransform)
    let duration = try await asset.load(.duration)

    // Presentation size after the track's rotation is applied.
    let shown = natural.applying(pref)
    let showW = abs(shown.width), showH = abs(shown.height)
    let scale = CGFloat(outW) / showW
    let scaledH = showH * scale
    let yOffset = (CGFloat(outH) - scaledH) / 2   // centre-crop/letterbox if the ratio is not exactly 9:16

    let comp = AVMutableVideoComposition()
    comp.renderSize = CGSize(width: outW, height: outH)
    comp.frameDuration = CMTime(value: 1, timescale: fps)
    let instr = AVMutableVideoCompositionInstruction()
    instr.timeRange = CMTimeRange(start: .zero, duration: duration)
    let layer = AVMutableVideoCompositionLayerInstruction(assetTrack: vTrack)
    layer.setTransform(pref.concatenating(CGAffineTransform(scaleX: scale, y: scale)).concatenating(CGAffineTransform(translationX: 0, y: yOffset)), at: .zero)
    instr.layerInstructions = [layer]
    comp.instructions = [instr]

    let reader = try AVAssetReader(asset: asset)
    let vOut = AVAssetReaderVideoCompositionOutput(videoTracks: [vTrack], videoSettings: [
        kCVPixelBufferPixelFormatTypeKey as String: kCVPixelFormatType_32BGRA,
    ])
    vOut.videoComposition = comp
    reader.add(vOut)

    var aOut: AVAssetReaderTrackOutput?
    if let aTrack = try await asset.loadTracks(withMediaType: .audio).first {
        let o = AVAssetReaderTrackOutput(track: aTrack, outputSettings: [AVFormatIDKey: kAudioFormatLinearPCM])
        reader.add(o)
        aOut = o
    }

    let writer = try AVAssetWriter(outputURL: outURL, fileType: .mp4)
    writer.shouldOptimizeForNetworkUse = true   // faststart
    let vIn = AVAssetWriterInput(mediaType: .video, outputSettings: [
        AVVideoCodecKey: AVVideoCodecType.h264,
        AVVideoWidthKey: outW,
        AVVideoHeightKey: outH,
        AVVideoCompressionPropertiesKey: [
            AVVideoAverageBitRateKey: kbps * 1000,
            AVVideoProfileLevelKey: AVVideoProfileLevelH264MainAutoLevel,
            AVVideoMaxKeyFrameIntervalKey: Int(fps) * 2,
            AVVideoExpectedSourceFrameRateKey: Int(fps),
        ] as [String: Any],
    ])
    vIn.expectsMediaDataInRealTime = false
    writer.add(vIn)

    var aIn: AVAssetWriterInput?
    if aOut != nil {
        let i = AVAssetWriterInput(mediaType: .audio, outputSettings: [
            AVFormatIDKey: kAudioFormatMPEG4AAC,
            AVNumberOfChannelsKey: 2,
            AVSampleRateKey: 44100,
            AVEncoderBitRateKey: 64000,
        ])
        i.expectsMediaDataInRealTime = false
        writer.add(i)
        aIn = i
    }

    reader.startReading()
    writer.startWriting()
    writer.startSession(atSourceTime: .zero)

    func pump(_ input: AVAssetWriterInput, _ output: AVAssetReaderOutput, _ queueName: String) async {
        await withCheckedContinuation { (cont: CheckedContinuation<Void, Never>) in
            let q = DispatchQueue(label: queueName)
            input.requestMediaDataWhenReady(on: q) {
                while input.isReadyForMoreMediaData {
                    if let buf = output.copyNextSampleBuffer() {
                        input.append(buf)
                    } else {
                        input.markAsFinished()
                        cont.resume()
                        return
                    }
                }
            }
        }
    }

    async let v: Void = pump(vIn, vOut, "video")
    if let aIn = aIn, let aOut = aOut {
        async let a: Void = pump(aIn, aOut, "audio")
        _ = await (v, a)
    } else {
        await v
    }
    await writer.finishWriting()
    if writer.status != .completed { fatalError("write failed: \(String(describing: writer.error))") }
    print("done: \(outURL.path)")
}

let sem = DispatchSemaphore(value: 0)
Task {
    do { try await run() } catch { print("error: \(error)"); exit(1) }
    sem.signal()
}
sem.wait()
