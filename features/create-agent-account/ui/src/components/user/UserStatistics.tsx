import { useEffect, useState } from "react";
import { Users } from "lucide-react";
import { getUserStatistics } from "../../api/userApi";


interface StatisticsData {

    totalUsers:number;

    customerServiceCount:number;

    adminCount:number;

    disabledCount:number;

}



export default function UserStatistics(){


    const [statistics,setStatistics] =
        useState<StatisticsData>({

            totalUsers:0,

            customerServiceCount:0,

            adminCount:0,

            disabledCount:0

        });



    useEffect(()=>{


        loadStatistics();


    },[]);



    async function loadStatistics(){


        try{


            const response =
                await getUserStatistics();


            setStatistics(
                response.data
            );


        }catch(error){


            console.error(
                "获取统计失败",
                error
            );


        }


    }



    const cards = [

        {

            title:"总用户数",

            value:
            statistics.totalUsers


        },


        {

            title:"本页客服人员",

            value:
            statistics.customerServiceCount


        },


        {

            title:"本页系统管理员",

            value:
            statistics.adminCount


        },


        {

            title:"本页禁用账号",

            value:
            statistics.disabledCount


        }

    ];




    return (

        <div className="statistics-container">


            {
                cards.map(
                    (card,index)=>(

                    <div
                        className="statistics-card"
                        key={index}
                    >


                        <div>

                            <span>
                                {card.title}
                            </span>


                            <h2>
                                {card.value}
                            </h2>


                            <p>
                                较上月 0
                            </p>


                        </div>


                        <div className="statistics-icon">

                            <Users size={28}/>

                        </div>


                    </div>


                    )
                )
            }


        </div>

    );


}