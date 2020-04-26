//Author: Mohit Bhole
//Project: Sensory Cuffs with Autism Treatment Center, Dallas
//Engineering Project in COmmunity Service

import React from "react";
import {
  Text,
  View,
  Dimensions,
  TextInput,
  StyleSheet,
  TouchableOpacity,
  Picker
} from "react-native";
import Constants from "expo-constants";
import { FloatingAction } from "react-native-floating-action";
import { AntDesign } from "@expo/vector-icons";
import * as SecureStore from "expo-secure-store";
const { width, height } = Dimensions.get("window");

list = [];

export default class AddNewProfile extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      name: "",
      pressureValue: 1,
      firstRender: true
    };
  }

  createListOfPressures() {
    list = [];
    for (let i = 1; i <= 100; i++) {
      list.push(
        <Picker.Item label={i.toString()} value={i} key={i.toString()} />
      );
    }
    return list;
  }

  async deleteProfile() {
    return;
  }

  async save() {
    let profiles = JSON.parse(await SecureStore.getItemAsync("Profiles"));
    profiles.push({
      name: this.state.name,
      pressureValue: this.state.pressureValue
    });
    SecureStore.setItemAsync("Profiles", JSON.stringify(profiles));
    this.props.navigation.reset();
  }

  render() {
    if (this.state.firstRender == true) {
      let selected = this.props.route.params.selectedProfile;
      console.log(selected);
      if (selected != null) {
        this.state.name = selected.name;
        this.state.pressureValue = selected.pressureValue;
      }
      this.state.firstRender = false;
    }

    return (
      <View
        style={{
          flex: 1,
          backgroundColor: "#fff",
          paddingBottom: height * 0.09
        }}
      >
        <View style={{ height: Constants.statusBarHeight }}></View>

        <View
          style={{
            justifyContent: "center",
            alignItems: "center",
            marginTop: height * 0.15
          }}
        >
          <Text
            style={{
              color: "#303030",
              fontWeight: "bold",
              fontSize: height * 0.045
            }}
          >
            Edit/Add Profile
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

        <View
          style={{
            width: width,
            marginTop: height * 0.16,
            flexDirection: "row",
            alignItems: "center",
            justifyContent: "center"
          }}
        >
          <Text style={{ fontSize: height * 0.018 }}>Name: </Text>
          <TextInput
            onChangeText={text => {
              this.setState({ name: text });
            }}
            value={this.state.name}
            style={{
              height: height * 0.036,
              borderColor: "#0f0f0f",
              borderWidth: 0.4,
              borderRadius: 5,
              marginLeft: width * 0.05,
              width: width * 0.52
            }}
          ></TextInput>
        </View>

        <View
          style={{
            width: width,
            marginTop: height * 0.04,
            flexDirection: "row",
            alignItems: "center",
            justifyContent: "center"
          }}
        >
          <Text style={{ fontSize: height * 0.018 }}>Pressure Value: </Text>
          <Picker
            selectedValue={this.state.pressureValue}
            style={{ height: 50, width: 100, marginLeft: width * 0.06 }}
            onValueChange={(itemValue, itemIndex) =>
              this.setState({ pressureValue: itemValue })
            }
          >
            {this.createListOfPressures()}
          </Picker>
        </View>

        <TouchableOpacity
          style={{
            width: width * 0.2,
            justifyContent: "center",
            alignItems: "center",
            marginTop: height * 0.01,
            alignSelf: "center"
          }}
          onPress={() => {
            this.save();
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
            <Text style={{ color: "#303030", fontWeight: "bold" }}>SAVE</Text>
          </View>
        </TouchableOpacity>

        <FloatingAction
          actions={[
            {
              text: "Delete Profile",
              icon: <AntDesign name="delete" size={23} color="#ffffff" />,
              name: "delete",
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
          overlayColor={"rgba(68, 68, 68, 0)"}
          onPressItem={name => {
            if (name == "back") {
              this.save();
              this.props.navigation.reset();
            } else if (name == "delete") {
              this.deleteProfile();
              this.props.navigation.reset();
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
