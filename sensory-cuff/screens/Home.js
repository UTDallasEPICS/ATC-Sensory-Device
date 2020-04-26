//Author: Mohit Bhole
//Project: Sensory Cuffs with Autism Treatment Center, Dallas
//Engineering Project in COmmunity Service

import React from "react";
import {
  StyleSheet,
  Text,
  View,
  TouchableOpacity,
  Dimensions,
  Slider,
  Image,
  Picker,
  ToastAndroid
} from "react-native";
import Constants from "expo-constants";
import { FloatingAction } from "react-native-floating-action";
import { AntDesign } from "@expo/vector-icons";
import * as SecureStore from "expo-secure-store";
const { width, height } = Dimensions.get("window");
list = [];

function GET(endpoint) {
  console.log("Getting " + endpoint)
  return fetch('http://192.168.43.194:81' + endpoint, {
  method: 'GET',
  methods: 'GET',
  headers: {
    Accept: 'application/json',
    'Content-Type': 'application/json',
  }
  });
}

export default class Home extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      time: 50,
      buttonLabel: "Turn on",
      profiles: [],
      selectedProfile: {}
    };
    this.initializeProfiles();
  }

  async initializeProfiles() {
    let tempProfiles = JSON.parse(await SecureStore.getItemAsync("Profiles"));
    if (tempProfiles == null) {
      SecureStore.setItemAsync("Profiles", JSON.stringify([]));
      this.setState({ profiles: [] });
      return;
    } else {
      this.setState({ profiles: tempProfiles });
      return;
    }
  }

  getVal(val) {
    console.log(val);
  }
  createProfileList() {
    list = [];
    for (i of this.state.profiles) {
      list.push(<Picker.Item label={i.name} value={i} key={i.name} />);
    }
    return list;
  }
  render() {
    return (
      <View
        style={{
          flex: 1,
          backgroundColor: "#fff",
          paddingBottom: height * 0.09
        }}
      >
        <View style={{ height: Constants.statusBarHeight }}></View>
        <Image
          resizeMode={"contain"}
          style={{ width: width, height: height * 0.32 }}
          source={require("../assets/mainmenupic.png")}
        />

        <View
          style={{
            justifyContent: "center",
            alignItems: "center",
            marginTop: height * 0.01
          }}
        >
          <Text
            style={{
              color: "#303030",
              fontWeight: "bold",
              fontSize: height * 0.045
            }}
          >
            Sensory Cuffs
          </Text>
          <Text
            style={{
              color: "#3366CC",
              fontSize: height * 0.014
            }}
          >
            Autism Treatment Center
          </Text>
        </View>

        <View style={styles.container}>
          <Text style={{ color: "#303030" }}>
            Please select the desired pressure and
          </Text>
          <Text style={{ color: "#303030" }}>press button to inflate</Text>
          <Picker
            selectedValue={this.state.selectedProfile}
            style={{ height: 50, width: 200, marginLeft: width * 0.06 }}
            onValueChange={(itemValue, itemIndex) =>
              this.setState({
                selectedProfile: itemValue,
                time: itemValue.pressureValue
              })
            }
          >
            <Picker.Item
              label={"Custom Pressure"}
              value={{ name: "Custom Pressure", pressureValue: "null" }}
              key={"Custom Pressure"}
            />
            {this.createProfileList()}
          </Picker>
          <Slider
            style={{ width: 300, marginTop: height * 0.06 }}
            step={1}
            minimumValue={1}
            maximumValue={100}
            value={this.state.time}
            onValueChange={val =>
              this.setState({
                selectedProfile: {
                  name: "Custom Pressure",
                  pressureValue: "null"
                },
                time: val
              })
            }
            onSlidingComplete={val => this.getVal(val)}
          />
          <View
            style={{
              justifyContent: "center",
              alignItems: "center",
              borderWidth: 1,
              borderColor: "grey",
              minWidth: width * 0.09,
              backgroundColor: "F5F5F5",
              paddingHorizontal: height * 0.01,
              paddingVertical: height * 0.01,
              borderRadius: 5,
              marginTop: height * 0.01
            }}
          >
            <Text style={{ color: "#303030" }}>{this.state.time}</Text>
          </View>
          <View
            style={{
              justifyContent: "center",
              alignItems: "center",
              flexDirection: "row"
            }}
          >
            <TouchableOpacity
              onPress={() => {
                if (this.state.buttonLabel == "Turn On") {
                  this.setState({ buttonLabel: "Turn Off" });
                  GET(`/turnon?time=` + this.state.time).then((res) => {
                    return res.json()}).then((res) => {
                      if(res){
                        ToastAndroid.show("Success", ToastAndroid.SHORT);
                        console.log(res);
                      } else {
                        ToastAndroid.show("Something went wrong", ToastAndroid.SHORT);
                        console.log(res);
                        this.setState({ buttonLabel: "Turn On" });
                      }
                  }).catch((err) => {
                    console.log(err)
                    console.log("Error connecting to server[GET]")
                    ToastAndroid.show("Error connecting to server", ToastAndroid.SHORT)
                  });
                } else {
                  this.setState({ buttonLabel: "Turn On" });
                  GET(`/turnon?time=0`).then((res) => {
                    return res.json()}).then((res) => {
                      if(res=="success"){
                        ToastAndroid.show("Success", ToastAndroid.SHORT);
                        console.log(res);
                      } else {
                        ToastAndroid.show("Something went wrong", ToastAndroid.SHORT);
                        console.log(res);
                        this.setState({ buttonLabel: "Turn Off" });
                      }
                  }).catch((err) => {
                    console.log(err)
                    console.log("Error connecting to server[GET]")
                    ToastAndroid.show("Error connecting to server", ToastAndroid.SHORT)
                  });
                }
              }}
            >
              <View
                style={{
                  backgroundColor: "#FFFF99",
                  paddingHorizontal: height * 0.02,
                  paddingVertical: height * 0.015,
                  borderRadius: 5,
                  marginTop: height * 0.04
                }}
              >
                <Text style={{ color: "#303030", fontWeight: "bold" }}>
                  {this.state.buttonLabel}
                </Text>
              </View>
            </TouchableOpacity>
          </View>
        </View>
        <FloatingAction
          actions={[]}
          color={"#fafafa"}
          floatingIcon={<AntDesign name="profile" size={28} color="green" />}
          overlayColor={"rgba(68, 68, 68, 0)"}
          onPressMain={() => {
            this.props.navigation.navigate("Profiles");
          }}
        />
      </View>
    );
  }
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: "#fff",
    alignItems: "center",
    justifyContent: "flex-end"
  }
});
