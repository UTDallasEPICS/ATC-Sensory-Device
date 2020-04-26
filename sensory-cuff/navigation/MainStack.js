//Author: Mohit Bhole
//Project: Sensory Cuffs with Autism Treatment Center, Dallas
//Engineering Project in COmmunity Service

import React from "react";
import { createStackNavigator } from '@react-navigation/stack';
import Home from '../screens/Home.js';
import Profiles from '../screens/Profiles.js';
import AddNewProfile from '../screens/AddNewProfile.js'
const Stack = createStackNavigator();

export default function  MyStack() {
  return (
    <Stack.Navigator>
      <Stack.Screen name="Home" component={Home} options={{headerShown:false}}/>
      <Stack.Screen name="Profiles" component={Profiles} options={{headerShown:false}}/>
      <Stack.Screen name="AddNewProfile" component={AddNewProfile} options={{headerShown:false}}/>
    </Stack.Navigator>
  );
}