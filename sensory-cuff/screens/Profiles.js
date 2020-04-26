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
  FlatList,
  Image
} from "react-native";
import Constants from "expo-constants";
import { FloatingAction } from "react-native-floating-action";
import { AntDesign } from "@expo/vector-icons";
import * as SecureStore from "expo-secure-store";
const { width, height } = Dimensions.get("window");
export default class Home extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      time: 50,
      buttonLabel: "Turn on",
      profiles: []
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

  componentWillFocus() {
    this.initializeProfiles();
  }

  getVal(val) {
    console.log(val);
  }

  async deleteProfile(index){
    let profiles = JSON.parse(
      await SecureStore.getItemAsync("Profiles")
    );
    console.log(profiles)
    profiles.splice(index,1);
    console.log(profiles)
    await SecureStore.setItemAsync(
      "Profiles",
      JSON.stringify(profiles)
    );
  }

  render() {
    return (
      <View
        style={{
          flex: 1,
          backgroundColor: "#fff",
          paddingBottom: height * 0.06
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
            Pressure Profiles
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

        <FlatList
          data={this.state.profiles}
          style={{ marginTop: height * 0.05 }}
          renderItem={({ item,index }) => {
            return (
              <TouchableOpacity
                style={{
                  borderColor: "#505050",
                  flexDirection: "row",
                  width: width * 0.92,
                  justifyContent: "space-between",
                  alignItems: "center",
                  paddingHorizontal: width * 0.06,
                  paddingVertical: height * 0.008,
                  backgroundColor: "#F5F5F5",
                  marginBottom: height * 0.005,
                  marginHorizontal: width * 0.04,
                  borderRadius: 8
                }}
                onPress={async () => {
                  this.props.navigation.navigate("AddNewProfile", {
                    selectedProfile: item
                  });
                  this.deleteProfile(index);
                }}
              >
                <Text
                  style={{
                    fontWeight: "bold",
                    color: "#303030",
                    fontSize: height * 0.022
                  }}
                >
                  {item.name}
                </Text>
                <Text
                  style={{
                    fontWeight: "bold",
                    color: "#3366CC",
                    fontSize: height * 0.022
                  }}
                >
                  {item.pressureValue}
                </Text>
              </TouchableOpacity>
            );
          }}
          keyExtractor={item => item.name}
        />

        <FloatingAction
          actions={[
            {
              text: "Add a new profile",
              icon: <AntDesign name="adduser" size={23} color="#ffffff" />,
              name: "add",
              position: 2,
              color: "green"
            },
            {
              text: "Go Back",
              icon: <AntDesign name="home" size={23} color="#ffffff" />,
              name: "back",
              position: 1,
              color: "green"
            }
          ]}
          color={"#505050"}
          //   floatingIcon={<AntDesign name="plus" size={28} color="green" />}
          overlayColor={"rgba(68, 68, 68, 0)"}
          onPressItem={name => {
            if (name == "back") {
              this.props.navigation.navigate("Home");
            } else {
              this.props.navigation.navigate("AddNewProfile",{selectedProfile: {name: "", pressureValue: 1}});
            }
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
