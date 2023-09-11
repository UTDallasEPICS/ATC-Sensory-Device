//
//  SwiftUIView.swift
//  ATC
//
//  Created by RAVI KANDARPA on 12/11/22.
//

import SwiftUI

struct SwiftUIView: View {
    @State var sliderValue: Double = 5
    @State var color: Color = .blue
    var body: some View {
        VStack {
            Text("Rating")
            Text(
                String(format: "%.f", sliderValue)
            )
            Slider(value: $sliderValue,
                   in: 1...7, step: 1.0,
                   onEditingChanged: {(_) in
                        color = .green
                    },
                   minimumValueLabel: Text("1")
                .font(.largeTitle)
                .foregroundColor(.orange),
                   maximumValueLabel: Text("7"),
                   label: {
                
                    })
                .accentColor(.blue)
        }
    }
}

struct SwiftUIView_Previews: PreviewProvider {
    static var previews: some View {
        SwiftUIView()
    }
}
